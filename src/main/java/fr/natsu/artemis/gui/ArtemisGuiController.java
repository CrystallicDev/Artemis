package fr.natsu.artemis.gui;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Ouvre les écrans Artemis en dehors du flux de traitement des commandes : une commande pose une
 * demande d'ouverture, réalisée au tick suivant. Cela évite que le chat, en se refermant après
 * l'exécution de la commande, n'écrase l'écran ouvert.
 */
public final class ArtemisGuiController {

    private static volatile boolean openConfigRequested;

    public static void requestOpenConfig() {
        openConfigRequested = true;
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !openConfigRequested) {
            return;
        }
        openConfigRequested = false;
        Artemis.LOGGER.info("[Config] ouverture de l'ecran de configuration");
        Minecraft.getMinecraft().displayGuiScreen(new GuiHudConfig());
    }
}
