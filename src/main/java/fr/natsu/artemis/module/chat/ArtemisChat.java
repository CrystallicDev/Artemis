package fr.natsu.artemis.module.chat;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.module.nametag.AdventureText;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ChatComponentText;

/**
 * Artemis custom chat: receives rich (hex) messages on its own plugin-message channel {@link #CHANNEL},
 * kept separate from Apollo so it doesn't depend on Apollo's release cycle.
 *
 * <p>Messages are injected into the vanilla chat through
 * {@code GuiNewChat.printChatMessageWithOptionalDeletion} (so position, scroll, fade and per-id
 * removal are handled natively). Hex is encoded in the {@code §x} format and rendered by
 * {@code MixinFontRenderer} + {@link HexTextRenderer}.</p>
 *
 * <p>Payload: {@code byte opcode}, then 0 = display ({@code int id}, {@code UTF json}),
 * 1 = remove ({@code int id}), 2 = clear.</p>
 */
public final class ArtemisChat {

    /** Dedicated channel. Registering it also tells the server this is an Artemis client. */
    public static final String CHANNEL = "artemis:chat";

    private static final int OP_DISPLAY = 0;
    private static final int OP_REMOVE = 1;
    private static final int OP_CLEAR = 2;

    /** Ids of the active Artemis messages (used by clear). */
    private static final Set<Integer> ACTIVE_IDS = ConcurrentHashMap.newKeySet();

    private ArtemisChat() {
    }

    /** Handles a payload received on {@link #CHANNEL} (called on the Netty thread). */
    public static void handleIncoming(byte[] payload) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            int opcode = in.readUnsignedByte();
            switch (opcode) {
                case OP_DISPLAY:
                    display(in.readInt(), in.readUTF());
                    break;
                case OP_REMOVE:
                    remove(in.readInt());
                    break;
                case OP_CLEAR:
                    clear();
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Chat] unreadable payload: {}", e.toString());
        }
    }

    private static void display(int id, String adventureJson) {
        String encoded = encode(AdventureText.parse(adventureJson));
        ACTIVE_IDS.add(id);
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (id != 0) {
                mc.ingameGUI.getChatGUI().deleteChatLine(id); // replace an existing message
            }
            mc.ingameGUI.getChatGUI().printChatMessageWithOptionalDeletion(new ChatComponentText(encoded), id);
        });
    }

    private static void remove(int id) {
        ACTIVE_IDS.remove(id);
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> mc.ingameGUI.getChatGUI().deleteChatLine(id));
    }

    private static void clear() {
        Minecraft mc = Minecraft.getMinecraft();
        Integer[] ids = ACTIVE_IDS.toArray(new Integer[0]);
        ACTIVE_IDS.clear();
        mc.addScheduledTask(() -> {
            for (int id : ids) {
                mc.ingameGUI.getChatGUI().deleteChatLine(id);
            }
        });
    }

    /** Encodes the runs into a chat string with hex colors ({@code §x}) plus § styles. */
    private static String encode(List<AdventureText.Run> runs) {
        StringBuilder sb = new StringBuilder();
        for (AdventureText.Run run : runs) {
            sb.append(HexTextRenderer.hexPrefix(run.rgb)).append(run.display);
        }
        return sb.toString();
    }
}
