package fr.natsu.artemis.render;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Triggers the Glowing pass 2 (compositing the outline onto the screen) after the HUD is drawn, like
 * Eterion's {@code ShaderListener}.
 */
public final class GlowOverlayListener {

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            GlowRenderer.displayFramebuffer();
        }
    }
}
