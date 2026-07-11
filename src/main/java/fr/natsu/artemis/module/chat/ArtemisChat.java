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
 * Chat Custom Artemis : réception des messages riches (hex) sur le canal plugin-message dédié
 * {@link #CHANNEL}, séparé d'Apollo pour ne pas dépendre de son cycle de mise à jour.
 *
 * <p>Les messages sont injectés dans le chat vanilla via
 * {@code GuiNewChat.printChatMessageWithOptionalDeletion} (donc position, scroll, fondu et
 * suppression par id gérés nativement). Le hex est encodé au format {@code §x} et rendu par
 * {@code MixinGuiNewChat} + {@link HexTextRenderer}.</p>
 *
 * <p>Payload : {@code byte opcode} — 0 = display ({@code int id}, {@code UTF json}),
 * 1 = remove ({@code int id}), 2 = clear.</p>
 */
public final class ArtemisChat {

    /** Canal dédié. L'enregistrer signale aussi au serveur que le client est un client Artemis. */
    public static final String CHANNEL = "artemis:chat";

    private static final int OP_DISPLAY = 0;
    private static final int OP_REMOVE = 1;
    private static final int OP_CLEAR = 2;

    /** Ids des messages Artemis actifs (pour le clear). */
    private static final Set<Integer> ACTIVE_IDS = ConcurrentHashMap.newKeySet();

    private ArtemisChat() {
    }

    /** Traite un payload reçu sur {@link #CHANNEL} (appelé sur le thread Netty). */
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
            Artemis.LOGGER.warn("[Chat] payload illisible : {}", e.toString());
        }
    }

    private static void display(int id, String adventureJson) {
        String encoded = encode(AdventureText.parse(adventureJson));
        ACTIVE_IDS.add(id);
        Minecraft mc = Minecraft.getMinecraft();
        mc.addScheduledTask(() -> {
            if (id != 0) {
                mc.ingameGUI.getChatGUI().deleteChatLine(id); // remplace un message existant
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

    /** Encode les runs en une chaîne chat avec couleurs hex ({@code §x}) + styles §. */
    private static String encode(List<AdventureText.Run> runs) {
        StringBuilder sb = new StringBuilder();
        for (AdventureText.Run run : runs) {
            sb.append(HexTextRenderer.hexPrefix(run.rgb)).append(run.display);
        }
        return sb.toString();
    }
}
