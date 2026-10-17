package fr.natsu.artemis.render;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.beam.BeamState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class BeamRenderer {

    private static final int BEAM_HEIGHT = 256;
    private static final float CORE_HALF_WIDTH = 0.15F;
    private static final float HALO_HALF_WIDTH = 0.35F;
    private static final float CORE_ALPHA = 0.60F;
    private static final float HALO_ALPHA = 0.15F;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (BeamState.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();

        Vec3 camera = new Vec3(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        for (BeamState.Beam beam : BeamState.all()) {
            addBeam(worldRenderer, beam, camera);
        }
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void addBeam(WorldRenderer wr, BeamState.Beam beam, Vec3 camera) {
        float r = ((beam.argb >> 16) & 0xFF) / 255.0F;
        float g = ((beam.argb >> 8) & 0xFF) / 255.0F;
        float b = (beam.argb & 0xFF) / 255.0F;
        double cx = beam.x + 0.5D - camera.xCoord;
        double cy = beam.y - camera.yCoord;
        double cz = beam.z + 0.5D - camera.zCoord;

        crossedQuads(wr, cx, cy, cz, CORE_HALF_WIDTH, r, g, b, CORE_ALPHA);
        crossedQuads(wr, cx, cy, cz, HALO_HALF_WIDTH, r, g, b, HALO_ALPHA);
    }

    private static void crossedQuads(WorldRenderer wr, double cx, double cy, double cz, float halfWidth,
                                     float r, float g, float b, float a) {
        double top = cy + BEAM_HEIGHT;
        wr.pos(cx - halfWidth, cy, cz).color(r, g, b, a).endVertex();
        wr.pos(cx + halfWidth, cy, cz).color(r, g, b, a).endVertex();
        wr.pos(cx + halfWidth, top, cz).color(r, g, b, a).endVertex();
        wr.pos(cx - halfWidth, top, cz).color(r, g, b, a).endVertex();
        wr.pos(cx, cy, cz - halfWidth).color(r, g, b, a).endVertex();
        wr.pos(cx, cy, cz + halfWidth).color(r, g, b, a).endVertex();
        wr.pos(cx, top, cz + halfWidth).color(r, g, b, a).endVertex();
        wr.pos(cx, top, cz - halfWidth).color(r, g, b, a).endVertex();
    }
}
