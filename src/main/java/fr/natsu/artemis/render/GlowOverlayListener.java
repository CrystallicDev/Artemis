package fr.natsu.artemis.render;

import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Déclenche la passe 2 du Glowing (composition du contour à l'écran) après le rendu du HUD, comme
 * le {@code ShaderListener} d'Eterion.
 */
public final class GlowOverlayListener {

    @SubscribeEvent
    public void onRenderOverlayPost(RenderGameOverlayEvent.Post event) {
        if (event.type == RenderGameOverlayEvent.ElementType.ALL) {
            GlowRenderer.displayFramebuffer();
        }
    }
}
