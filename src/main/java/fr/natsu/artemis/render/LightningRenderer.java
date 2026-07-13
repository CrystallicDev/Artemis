package fr.natsu.artemis.render;

import java.util.List;
import java.util.Random;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.lightning.LightningState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the Artemis colored lightning bolts ({@link LightningState}) on {@code RenderWorldLastEvent}.
 *
 * <p>Replicates vanilla lightning geometry (4 layers × 3 segments × 8 nodes) but in
 * {@code POSITION_COLOR} with two colors: a core (inner, bright layers) and an outer glow. The bolt's
 * seed freezes its shape for its whole lifetime. Adapted from Eterion's {@code CustomLightningRenderer}.</p>
 *
 * <p>Blending is <b>additive</b> for the glow. We reset the blend func to the default afterwards,
 * otherwise it lingers in the GlStateManager cache and washes out the HUD text (only visible in
 * spectator, where the hotbar doesn't reset the state).</p>
 */
public final class LightningRenderer {

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (LightningState.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null || mc.theWorld == null) {
            return;
        }

        List<LightningState.Bolt> bolts = LightningState.active();
        if (bolts.isEmpty()) {
            return;
        }

        float partialTicks = event.partialTicks;
        double camX = viewer.lastTickPosX + (viewer.posX - viewer.lastTickPosX) * partialTicks;
        double camY = viewer.lastTickPosY + (viewer.posY - viewer.lastTickPosY) * partialTicks;
        double camZ = viewer.lastTickPosZ + (viewer.posZ - viewer.lastTickPosZ) * partialTicks;
        long now = System.currentTimeMillis();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();

        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0); // additive

        for (LightningState.Bolt bolt : bolts) {
            float alpha = bolt.alpha(now);
            if (alpha <= 0.0F) {
                continue;
            }
            GlStateManager.pushMatrix();
            GlStateManager.translate(bolt.x - camX, bolt.y - camY, bolt.z - camZ);
            renderBolt(tessellator, worldRenderer, bolt, alpha);
            GlStateManager.popMatrix();
        }

        // Put the func back to the default BEFORE turning blending off (see the class javadoc).
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        // Do NOT re-enable lighting (it's off at RenderWorldLastEvent): leaving it on darkens the next
        // 2D pass (black enchantment glint, greyed-out HUD text).
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Geometry of one bolt (taken from vanilla), two-colored core/outer, master alpha applied. */
    private static void renderBolt(Tessellator tessellator, WorldRenderer worldRenderer,
            LightningState.Bolt bolt, float alpha) {
        double[] baseX = new double[8];
        double[] baseZ = new double[8];
        double d0 = 0.0D;
        double d1 = 0.0D;
        Random random = new Random(bolt.seed);

        for (int i = 7; i >= 0; --i) {
            baseX[i] = d0;
            baseZ[i] = d1;
            d0 += random.nextInt(11) - 5;
            d1 += random.nextInt(11) - 5;
        }

        for (int layer = 0; layer < 4; ++layer) {
            Random layerRandom = new Random(bolt.seed);

            for (int segmentType = 0; segmentType < 3; ++segmentType) {
                int segmentStart = segmentType > 0 ? 7 - segmentType : 7;
                int segmentEnd = segmentType > 0 ? segmentStart - 2 : 0;

                double offsetX = baseX[segmentStart] - d0;
                double offsetZ = baseZ[segmentStart] - d1;

                for (int segment = segmentStart; segment >= segmentEnd; --segment) {
                    double prevOffsetX = offsetX;
                    double prevOffsetZ = offsetZ;

                    if (segmentType == 0) {
                        offsetX += layerRandom.nextInt(11) - 5;
                        offsetZ += layerRandom.nextInt(11) - 5;
                    } else {
                        offsetX += layerRandom.nextInt(31) - 15;
                        offsetZ += layerRandom.nextInt(31) - 15;
                    }

                    // Inner layers = bright core, outer layers = glow.
                    float red = layer < 2 ? bolt.coreRed : bolt.mainRed;
                    float green = layer < 2 ? bolt.coreGreen : bolt.mainGreen;
                    float blue = layer < 2 ? bolt.coreBlue : bolt.mainBlue;

                    float layerAlpha = alpha * LAYER_ALPHA[layer];

                    double width1 = 0.1D + layer * 0.2D;
                    double width2 = 0.1D + layer * 0.2D;
                    if (segmentType == 0) {
                        width1 *= segment * 0.1D + 1.0D;
                        width2 *= (segment - 1) * 0.1D + 1.0D;
                    }

                    worldRenderer.begin(GL11.GL_TRIANGLE_STRIP, DefaultVertexFormats.POSITION_COLOR);
                    for (int corner = 0; corner < 5; ++corner) {
                        double x1 = 0.5D - width1;
                        double z1 = 0.5D - width1;
                        double x2 = 0.5D - width2;
                        double z2 = 0.5D - width2;
                        if (corner == 1 || corner == 2) {
                            x1 += width1 * 2.0D;
                            x2 += width2 * 2.0D;
                        }
                        if (corner == 2 || corner == 3) {
                            z1 += width1 * 2.0D;
                            z2 += width2 * 2.0D;
                        }

                        worldRenderer.pos(x2 + offsetX, segment * 16, z2 + offsetZ)
                            .color(red, green, blue, layerAlpha).endVertex();
                        worldRenderer.pos(x1 + prevOffsetX, (segment + 1) * 16, z1 + prevOffsetZ)
                            .color(red, green, blue, layerAlpha).endVertex();
                    }
                    tessellator.draw();
                }
            }
        }
    }

    /** Per-layer alpha falloff (bright core -> diffuse outer glow). */
    private static final float[] LAYER_ALPHA = {0.8F, 0.6F, 0.4F, 0.3F};
}
