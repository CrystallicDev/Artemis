package fr.natsu.artemis.render;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.border.BorderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class BorderRenderer {

    private static final ResourceLocation FORCEFIELD = new ResourceLocation("textures/misc/forcefield.png");

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (BorderState.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();

        Vec3 camera = new Vec3(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        double renderDistance = mc.gameSettings.renderDistanceChunks * 16.0D;
        float scroll = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F;

        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ONE, GL11.GL_ZERO);
        mc.getTextureManager().bindTexture(FORCEFIELD);
        GlStateManager.depthMask(false);
        GlStateManager.doPolygonOffset(-3.0F, -3.0F);
        GlStateManager.enablePolygonOffset();
        GlStateManager.alphaFunc(516, 0.1F);
        GlStateManager.enableAlpha();
        GlStateManager.disableCull();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        for (BorderState.Border border : BorderState.all()) {
            double[] b = border.bounds();
            int argb = border.argb;
            int alpha = (argb >>> 24) & 0xFF;
            GlStateManager.color(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, alpha == 0 ? 1.0F : alpha / 255.0F);

            worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
            wallAlongX(worldRenderer, b[1], b[0], b[2], camera, renderDistance, scroll);
            wallAlongX(worldRenderer, b[3], b[0], b[2], camera, renderDistance, scroll);
            wallAlongZ(worldRenderer, b[0], b[1], b[3], camera, renderDistance, scroll);
            wallAlongZ(worldRenderer, b[2], b[1], b[3], camera, renderDistance, scroll);
            tessellator.draw();
        }

        GlStateManager.enableCull();
        GlStateManager.doPolygonOffset(0.0F, 0.0F);
        GlStateManager.disablePolygonOffset();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        GlStateManager.depthMask(true);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void wallAlongX(WorldRenderer wr, double z, double x0, double x1, Vec3 camera,
                                   double renderDistance, float scroll) {
        double lo = Math.max(x0, camera.xCoord - renderDistance);
        double hi = Math.min(x1, camera.xCoord + renderDistance);
        double topY = 256.0D - camera.yCoord;
        double botY = -camera.yCoord;
        double wz = z - camera.zCoord;
        float u = 0.0F;
        for (double x = lo; x < hi; x += 1.0D, u += 0.5F) {
            double seg = Math.min(1.0D, hi - x);
            float uSeg = (float) (seg * 0.5D);
            double wx0 = x - camera.xCoord;
            double wx1 = x + seg - camera.xCoord;
            wr.pos(wx0, topY, wz).tex(scroll + u, scroll).endVertex();
            wr.pos(wx1, topY, wz).tex(scroll + u + uSeg, scroll).endVertex();
            wr.pos(wx1, botY, wz).tex(scroll + u + uSeg, scroll + 128.0F).endVertex();
            wr.pos(wx0, botY, wz).tex(scroll + u, scroll + 128.0F).endVertex();
        }
    }

    private static void wallAlongZ(WorldRenderer wr, double x, double z0, double z1, Vec3 camera,
                                   double renderDistance, float scroll) {
        double lo = Math.max(z0, camera.zCoord - renderDistance);
        double hi = Math.min(z1, camera.zCoord + renderDistance);
        double topY = 256.0D - camera.yCoord;
        double botY = -camera.yCoord;
        double wx = x - camera.xCoord;
        float u = 0.0F;
        for (double z = lo; z < hi; z += 1.0D, u += 0.5F) {
            double seg = Math.min(1.0D, hi - z);
            float uSeg = (float) (seg * 0.5D);
            double wz0 = z - camera.zCoord;
            double wz1 = z + seg - camera.zCoord;
            wr.pos(wx, topY, wz0).tex(scroll + u, scroll).endVertex();
            wr.pos(wx, topY, wz1).tex(scroll + u + uSeg, scroll).endVertex();
            wr.pos(wx, botY, wz1).tex(scroll + u + uSeg, scroll + 128.0F).endVertex();
            wr.pos(wx, botY, wz0).tex(scroll + u, scroll + 128.0F).endVertex();
        }
    }
}
