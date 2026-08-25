package fr.natsu.artemis.module.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.module.nametag.AdventureText;
import fr.natsu.artemis.module.nametag.AdventureText.Run;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public final class NotificationModule {

    private static final String DISPLAY = "lunarclient.apollo.notification.v1.DisplayNotificationMessage";
    private static final String RESET = "lunarclient.apollo.notification.v1.ResetNotificationsMessage";

    private static final long DEFAULT_DISPLAY_MILLIS = 5000L;
    private static final int TITLE_RGB = 0xFFFFFF;
    private static final int DESCRIPTION_RGB = 0xAAAAAA;

    private NotificationModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, NotificationModule::onDisplay);
        network.register(RESET, NotificationModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String plainTitle = null;
        String plainDescription = null;
        String resourceLocation = null;
        long displayMillis = 0L;
        List<String> titleJson = new ArrayList<>();
        List<String> descriptionJson = new ArrayList<>();
        ItemStack itemIcon = null;

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    plainTitle = in.readString();
                    break;
                case 2:
                    plainDescription = in.readString();
                    break;
                case 3:
                    resourceLocation = in.readString();
                    break;
                case 4:
                    displayMillis = ApolloProtos.parseDurationMillis(in.readBytes());
                    break;
                case 5:
                    titleJson.add(in.readString());
                    break;
                case 6:
                    descriptionJson.add(in.readString());
                    break;
                case 7:
                    itemIcon = parseIcon(in.readBytes());
                    break;
                default:
                    in.skipField(tag);
            }
        }

        List<List<Run>> titleLines = buildLines(titleJson, plainTitle, TITLE_RGB);
        List<List<Run>> descriptionLines = buildLines(descriptionJson, plainDescription, DESCRIPTION_RGB);
        if (titleLines.isEmpty() && descriptionLines.isEmpty()) {
            return;
        }

        ResourceLocation textureIcon = null;
        if (itemIcon == null && resourceLocation != null && !resourceLocation.isEmpty()) {
            textureIcon = new ResourceLocation(resourceLocation);
        }
        long lifetime = displayMillis > 0L ? displayMillis : DEFAULT_DISPLAY_MILLIS;

        NotificationState.add(new NotificationState.Notification(
            titleLines, descriptionLines, itemIcon, textureIcon, System.currentTimeMillis(), lifetime));
        Artemis.LOGGER.info("[Notification] display title='{}' ({}ms)",
            plainTitle == null ? (titleJson.isEmpty() ? "" : titleJson.get(0)) : plainTitle, lifetime);
    }

    private static void onReset(Any message) {
        NotificationState.resetAll();
        Artemis.LOGGER.info("[Notification] reset");
    }

    private static List<List<Run>> buildLines(List<String> jsonLines, String plain, int plainRgb) {
        List<List<Run>> lines = new ArrayList<>();
        if (!jsonLines.isEmpty()) {
            for (String json : jsonLines) {
                List<Run> runs = AdventureText.parse(json);
                if (!runs.isEmpty()) {
                    lines.add(runs);
                }
            }
        } else if (plain != null && !plain.isEmpty()) {
            lines.add(Collections.singletonList(new Run(plain, plainRgb)));
        }
        return lines;
    }

    private static ItemStack parseIcon(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        ItemStack icon = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                icon = parseItemStackIcon(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }
        return icon;
    }

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
}
