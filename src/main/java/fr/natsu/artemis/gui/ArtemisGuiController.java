package fr.natsu.artemis.gui;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

/**
 * Opens the Artemis screens outside of the command-processing flow: a command sets an open request,
 * carried out on the next tick. This keeps the chat, which closes right after the command runs, from
 * overwriting the screen we just opened.
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
        Artemis.LOGGER.info("[Config] opening the configuration screen");
        Minecraft.getMinecraft().displayGuiScreen(new GuiHudConfig());
    }
}
