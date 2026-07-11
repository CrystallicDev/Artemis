package fr.natsu.artemis.module.marker;

import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * Module Marker : traduit {@code lunarclient.apollo.marker.v1.*} en état de {@link MarkerState}.
 * Rendu (icône du target + infos conditionnelles) assuré par {@code MarkerRenderer}.
 *
 * <p>{@code DisplayMarkerMessage} : id(#1), location(#2), owner_name(#4), flag(#5 type+couleur),
 * target(#6 oneof Item/Block/Entity/Player), style(#11). Les show_* du style sont des
 * {@code MarkerDisplayCondition} (NEVER/HOVER/ALWAYS).</p>
 */
public final class MarkerModule {

    private static final String DISPLAY = "lunarclient.apollo.marker.v1.DisplayMarkerMessage";
    private static final String REMOVE = "lunarclient.apollo.marker.v1.RemoveMarkerMessage";
    private static final String RESET = "lunarclient.apollo.marker.v1.ResetMarkersMessage";

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private MarkerModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, MarkerModule::onDisplay);
        network.register(REMOVE, MarkerModule::onRemove);
        network.register(RESET, MarkerModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        String ownerName = "";
        double[] loc = null;
        int type = MarkerState.TYPE_NORMAL;
        int iconColor = DEFAULT_COLOR;
        MarkerState.Target target = MarkerState.Target.none();
        Object[] style = defaultStyle();

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    id = in.readString();
                    break;
                case 2:
                    loc = parseLocation(in.readBytes());
                    break;
                case 4:
                    ownerName = in.readString();
                    break;
                case 5: {
                    int[] flag = parseFlag(in.readBytes());
                    type = flag[0];
                    iconColor = flag[1];
                    break;
                }
                case 6:
                    target = parseTarget(in.readBytes());
                    break;
                case 11:
                    style = parseStyle(in.readBytes());
                    break;
                default:
                    // owner_id(#3), duration(#7), notifs(#8/9/10) : ignorés.
                    in.skipField(tag);
            }
        }

        if (id == null || loc == null) {
            return;
        }
        MarkerState.display(new MarkerState.Marker(id, ownerName, loc[0], loc[1], loc[2], type, iconColor,
            target, (Integer) style[0], (Integer) style[1], (Integer) style[2], (Integer) style[3],
            (Integer) style[4], (Boolean) style[5], (Float) style[6]));
        Artemis.LOGGER.info("[Marker] display '{}' type={} target={} ({}, {}, {})",
            id, type, target.type, loc[0], loc[1], loc[2]);
    }

    private static void onRemove(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                id = in.readString();
            } else {
                in.skipField(tag);
            }
        }
        if (id != null) {
            MarkerState.remove(id);
            Artemis.LOGGER.info("[Marker] remove '{}'", id);
        }
    }

    private static void onReset(Any message) {
        MarkerState.resetAll();
        Artemis.LOGGER.info("[Marker] reset");
    }

    // ------------------------------------------------------------------
    // Flag (type + couleur)
    // ------------------------------------------------------------------

    private static int[] parseFlag(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int type = MarkerState.TYPE_NORMAL;
        int color = DEFAULT_COLOR;
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = WireFormat.getTagFieldNumber(tag);
            if (field >= 1 && field <= 4) {
                type = field;
                color = parseColorInMessage(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }
        return new int[] {type, color};
    }

    /** Un sous-message qui porte un {@code Color} en champ #1 (marker de type, ou ItemTarget). */
    private static int parseColorInMessage(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int color = DEFAULT_COLOR;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                color = ApolloProtos.parseColorArgb(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }
        return color;
    }

    // ------------------------------------------------------------------
    // Target (oneof Item/Block/Entity/Player)
    // ------------------------------------------------------------------

    private static MarkerState.Target parseTarget(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = WireFormat.getTagFieldNumber(tag);
            switch (field) {
                case 1:
                    return new MarkerState.Target(MarkerState.TARGET_ITEM,
                        parseTargetItem(in.readBytes()), null, null, null);
                case 2:
                    return new MarkerState.Target(MarkerState.TARGET_BLOCK,
                        parseTargetItem(in.readBytes()), null, null, null);
                case 3:
                    return new MarkerState.Target(MarkerState.TARGET_ENTITY,
                        null, parseEntityType(in.readBytes()), null, null);
                case 4: {
                    Object[] player = parsePlayerTarget(in.readBytes());
                    return new MarkerState.Target(MarkerState.TARGET_PLAYER,
                        null, null, (UUID) player[0], (String) player[1]);
                }
                default:
                    in.skipField(tag);
            }
        }
        return MarkerState.Target.none();
    }

    /** ItemTarget/BlockTarget : item_stack(#1 ItemStackIcon). */
    private static ItemStack parseTargetItem(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        ItemStack item = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                item = parseItemStackIcon(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }
        return item;
    }

    /** ItemStackIcon : item_id(#1 int32) OU item_name(#2 string). */
    private static ItemStack parseItemStackIcon(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int itemId = -1;
        String itemName = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    itemId = in.readInt32();
                    break;
                case 2:
                    itemName = in.readString();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        Item item = null;
        if (itemName != null && !itemName.isEmpty()) {
            item = Item.getByNameOrId(itemName);
        } else if (itemId >= 0) {
            item = Item.getItemById(itemId);
        }
        return item == null ? null : new ItemStack(item);
    }

    /** EntityTarget : entity_type(#1 string). */
    private static String parseEntityType(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        String entityType = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                entityType = in.readString();
            } else {
                in.skipField(tag);
            }
        }
        return entityType;
    }

    /** PlayerTarget : uuid(#1 Uuid), name(#2 string). Retourne [UUID, String]. */
    private static Object[] parsePlayerTarget(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        UUID playerId = null;
        String playerName = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    playerId = ApolloProtos.parseUuid(in.readBytes());
                    break;
                case 2:
                    playerName = in.readString();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new Object[] {playerId, playerName};
    }

    // ------------------------------------------------------------------
    // Style
    // ------------------------------------------------------------------

    private static Object[] defaultStyle() {
        // [showOwner, showCoords, showDistance, showDescription, ownerDisplay, textShadow, scale]
        return new Object[] {MarkerState.COND_UNSPECIFIED, MarkerState.COND_UNSPECIFIED,
            MarkerState.COND_UNSPECIFIED, MarkerState.COND_UNSPECIFIED, MarkerState.OWNER_NAME,
            Boolean.TRUE, 1.0F};
    }

    private static Object[] parseStyle(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        Object[] style = defaultStyle();
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    style[6] = in.readFloat();
                    break;
                case 4:
                    style[5] = in.readBool();
                    break;
                case 6:
                    style[4] = in.readEnum();
                    break;
                case 7:
                    style[0] = in.readEnum();
                    break;
                case 8:
                    style[1] = in.readEnum();
                    break;
                case 9:
                    style[2] = in.readEnum();
                    break;
                case 10:
                    style[3] = in.readEnum();
                    break;
                default:
                    // animate(#2), compact(#3), owner_suffix(#5), description_display(#11) : ignorés.
                    in.skipField(tag);
            }
        }
        return style;
    }

    private static double[] parseLocation(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        double x = 0;
        double y = 0;
        double z = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 2:
                    x = in.readDouble();
                    break;
                case 3:
                    y = in.readDouble();
                    break;
                case 4:
                    z = in.readDouble();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new double[] {x, y, z};
    }
}
