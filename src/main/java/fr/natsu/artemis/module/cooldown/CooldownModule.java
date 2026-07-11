package fr.natsu.artemis.module.cooldown;

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
 * Module Cooldown : traduit {@code lunarclient.apollo.cooldown.v1.*} en entrées de
 * {@link CooldownState}. Rendu HUD assuré par {@code CooldownRenderer}.
 *
 * <p>{@code DisplayCooldownMessage} : name(#1 string), duration(#2 Duration), icon(#3 Icon oneof),
 * style(#4 CooldownStyle = 4 couleurs). On gère l'icône de type ItemStack (item_id/item_name).</p>
 */
public final class CooldownModule {

    private static final String DISPLAY = "lunarclient.apollo.cooldown.v1.DisplayCooldownMessage";
    private static final String REMOVE = "lunarclient.apollo.cooldown.v1.RemoveCooldownMessage";
    private static final String RESET_ALL = "lunarclient.apollo.cooldown.v1.ResetCooldownsMessage";

    // Couleurs ARGB par défaut (reprises de Lunar) si le style n'en fournit pas.
    private static final int DEFAULT_CIRCLE_START = 0x99595959;
    private static final int DEFAULT_CIRCLE_END = 0xFFE5E5E5;
    private static final int DEFAULT_EDGE = 0xFF000000;
    private static final int DEFAULT_TEXT = 0xFFFFFFFF;

    private CooldownModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, CooldownModule::onDisplay);
        network.register(REMOVE, CooldownModule::onRemove);
        network.register(RESET_ALL, CooldownModule::onResetAll);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String name = null;
        long durationMillis = 0L;
        ItemStack icon = null;
        int circleStart = DEFAULT_CIRCLE_START;
        int circleEnd = DEFAULT_CIRCLE_END;
        int circleEdge = DEFAULT_EDGE;
        int textColor = DEFAULT_TEXT;

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    name = in.readString();
                    break;
                case 2:
                    durationMillis = ApolloProtos.parseDurationMillis(in.readBytes());
                    break;
                case 3:
                    icon = parseIcon(in.readBytes());
                    break;
                case 4: {
                    int[] style = parseStyle(in.readBytes());
                    circleStart = style[0];
                    circleEnd = style[1];
                    circleEdge = style[2];
                    textColor = style[3];
                    break;
                }
                default:
                    in.skipField(tag);
            }
        }

        if (name == null || durationMillis <= 0L) {
            return;
        }

        CooldownState.display(name, durationMillis, icon, circleStart, circleEnd, circleEdge, textColor);
        Artemis.LOGGER.info("[Cooldown] display '{}' {}ms icon={}", name, durationMillis,
            icon == null ? "aucune" : icon.getItem());
    }

    private static void onRemove(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String name = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                name = in.readString();
            } else {
                in.skipField(tag);
            }
        }
        if (name != null) {
            CooldownState.remove(name);
            Artemis.LOGGER.info("[Cooldown] remove '{}'", name);
        }
    }

    private static void onResetAll(Any message) {
        CooldownState.resetAll();
        Artemis.LOGGER.info("[Cooldown] reset complet");
    }

    /** Parse un {@code Icon} : seul le variant ItemStack (#1) est géré. */
    private static ItemStack parseIcon(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        ItemStack icon = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                icon = parseItemStackIcon(in.readBytes());
            } else {
                // Variants resource-location non gérés pour l'instant.
                in.skipField(tag);
            }
        }
        return icon;
    }

    /** Parse un {@code ItemStackIcon} : item_id(#1 int32) OU item_name(#2 string). */
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

    /** Parse un {@code CooldownStyle} : circle start/end/edge + text color (tous {@code Color} ARGB). */
    private static int[] parseStyle(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int[] colors = {DEFAULT_CIRCLE_START, DEFAULT_CIRCLE_END, DEFAULT_EDGE, DEFAULT_TEXT};
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = WireFormat.getTagFieldNumber(tag);
            if (field >= 1 && field <= 4) {
                colors[field - 1] = ApolloProtos.parseColorArgb(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }
        return colors;
    }
}
