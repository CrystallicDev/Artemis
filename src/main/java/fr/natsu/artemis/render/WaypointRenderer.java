package fr.natsu.artemis.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.waypoint.WaypointState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the Apollo waypoints: a colored vertical beam (beacon) at the block, and a label (name +
 * distance) projected to the screen, clamped to the edge when the waypoint is off-screen.
 */
public final class WaypointRenderer {

    private static final float BEAM_HALF_WIDTH = 0.15F;
    private static final float BEAM_HEIGHT = 300.0F;
    private static final float BEAM_ALPHA = 0.4F;
    private static final float EDGE_MARGIN = 16.0F;
    private static final double LABEL_HEIGHT = 1.6D;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (WaypointState.all().isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();

        Vec3 camera = new Vec3(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        ScaledResolution resolution = new ScaledResolution(mc);

        List<Object[]> labels = new ArrayList<>();
        for (WaypointState.Waypoint waypoint : WaypointState.all()) {
            if (waypoint.hidden) {
                continue;
            }
            double cx = waypoint.x + 0.5D - camera.xCoord;
            double cy = waypoint.y - camera.yCoord;
            double cz = waypoint.z + 0.5D - camera.zCoord;

            if (waypoint.showBeam) {
                renderBeam(cx, cy, cz, waypoint.colorArgb);
            }
            if (waypoint.highlightBlock) {
                renderBlockHighlight(waypoint.x - camera.xCoord, waypoint.y - camera.yCoord,
                    waypoint.z - camera.zCoord, waypoint.colorArgb, waypoint.lineWidth);
            }

            if (!waypoint.showText) {
                continue;
            }
            double[] screen = project(cx, cy + LABEL_HEIGHT, cz, resolution);
            if (screen != null) {
                int distance = (int) Math.round(viewer.getDistance(
                    waypoint.x + 0.5D, waypoint.y, waypoint.z + 0.5D));
                labels.add(new Object[] {(int) screen[0], (int) screen[1], waypoint, distance});
            }
        }

        if (labels.isEmpty()) {
            return;
        }

        int screenWidth = resolution.getScaledWidth();
        int screenHeight = resolution.getScaledHeight();
        setupOverlay(resolution);
        FontRenderer font = mc.fontRendererObj;
        for (Object[] label : labels) {
            int x = (int) Math.max(EDGE_MARGIN, Math.min(screenWidth - EDGE_MARGIN, (Integer) label[0]));
            int y = (int) Math.max(EDGE_MARGIN, Math.min(screenHeight - EDGE_MARGIN, (Integer) label[1]));
            WaypointState.Waypoint waypoint = (WaypointState.Waypoint) label[2];
            int distance = (Integer) label[3];

            int nameColor = 0xFF000000 | (waypoint.colorArgb & 0xFFFFFF);
            font.drawStringWithShadow(waypoint.name, x - font.getStringWidth(waypoint.name) / 2.0F, y, nameColor);
            if (waypoint.showDistance) {
                String dist = "(" + distance + "m)";
                font.drawStringWithShadow(dist, x - font.getStringWidth(dist) / 2.0F, y + 10.0F, 0xFFFFFFFF);
            }
        }
        cleanupOverlay();
    }

    private static void renderBeam(double rx, double ry, double rz, int argb) {
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(rx, ry, rz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.disableCull();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE, 1, 0);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(-BEAM_HALF_WIDTH, 0.0D, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(BEAM_HALF_WIDTH, 0.0D, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(BEAM_HALF_WIDTH, BEAM_HEIGHT, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(-BEAM_HALF_WIDTH, BEAM_HEIGHT, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, 0.0D, -BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, 0.0D, BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, BEAM_HEIGHT, BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, BEAM_HEIGHT, -BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void renderBlockHighlight(double rx, double ry, double rz, int argb, float lineWidth) {
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        GlStateManager.pushMatrix();
        GlStateManager.translate(rx, ry, rz);
        GlStateManager.disableTexture2D();
        GlStateManager.disableLighting();
        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, 1, 0);
        GlStateManager.depthMask(false);
        GlStateManager.disableDepth();
        GL11.glLineWidth(lineWidth <= 0.0F ? 2.0F : lineWidth);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION_COLOR);
        edge(worldRenderer, 0, 0, 0, 1, 0, 0, r, g, b);
        edge(worldRenderer, 1, 0, 0, 1, 0, 1, r, g, b);
        edge(worldRenderer, 1, 0, 1, 0, 0, 1, r, g, b);
        edge(worldRenderer, 0, 0, 1, 0, 0, 0, r, g, b);
        edge(worldRenderer, 0, 1, 0, 1, 1, 0, r, g, b);
        edge(worldRenderer, 1, 1, 0, 1, 1, 1, r, g, b);
        edge(worldRenderer, 1, 1, 1, 0, 1, 1, r, g, b);
        edge(worldRenderer, 0, 1, 1, 0, 1, 0, r, g, b);
        edge(worldRenderer, 0, 0, 0, 0, 1, 0, r, g, b);
        edge(worldRenderer, 1, 0, 0, 1, 1, 0, r, g, b);
        edge(worldRenderer, 1, 0, 1, 1, 1, 1, r, g, b);
        edge(worldRenderer, 0, 0, 1, 0, 1, 1, r, g, b);
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GL11.glLineWidth(1.0F);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void edge(WorldRenderer wr, double x1, double y1, double z1,
                             double x2, double y2, double z2, float r, float g, float b) {
        wr.pos(x1, y1, z1).color(r, g, b, 1.0F).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, 1.0F).endVertex();
    }

    private static final FloatBuffer MODELVIEW_BUF = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION_BUF = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT_BUF = BufferUtils.createIntBuffer(16);
    private static final float[] MODELVIEW = new float[16];
    private static final float[] PROJECTION = new float[16];
    private static final int[] VIEWPORT = new int[4];

    private static double[] project(double x, double y, double z, ScaledResolution resolution) {
        MODELVIEW_BUF.clear();
        PROJECTION_BUF.clear();
        VIEWPORT_BUF.clear();
        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, MODELVIEW_BUF);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, PROJECTION_BUF);
        GL11.glGetInteger(GL11.GL_VIEWPORT, VIEWPORT_BUF);
        MODELVIEW_BUF.get(MODELVIEW);
        PROJECTION_BUF.get(PROJECTION);
        VIEWPORT_BUF.get(VIEWPORT);

        float ex = MODELVIEW[0] * (float) x + MODELVIEW[4] * (float) y + MODELVIEW[8] * (float) z + MODELVIEW[12];
        float ey = MODELVIEW[1] * (float) x + MODELVIEW[5] * (float) y + MODELVIEW[9] * (float) z + MODELVIEW[13];
        float ez = MODELVIEW[2] * (float) x + MODELVIEW[6] * (float) y + MODELVIEW[10] * (float) z + MODELVIEW[14];
        float ew = MODELVIEW[3] * (float) x + MODELVIEW[7] * (float) y + MODELVIEW[11] * (float) z + MODELVIEW[15];

        float cx = PROJECTION[0] * ex + PROJECTION[4] * ey + PROJECTION[8] * ez + PROJECTION[12] * ew;
        float cy = PROJECTION[1] * ex + PROJECTION[5] * ey + PROJECTION[9] * ez + PROJECTION[13] * ew;
        float cw = PROJECTION[3] * ex + PROJECTION[7] * ey + PROJECTION[11] * ez + PROJECTION[15] * ew;

        if (cw <= 1.0E-4F) {
            return null;
        }

        float ndcX = cx / cw;
        float ndcY = cy / cw;
        float winX = VIEWPORT[0] + VIEWPORT[2] * (ndcX + 1.0F) / 2.0F;
        float winY = VIEWPORT[1] + VIEWPORT[3] * (ndcY + 1.0F) / 2.0F;

        double screenX = (winX / VIEWPORT[2]) * resolution.getScaledWidth();
        double screenY = resolution.getScaledHeight() - (winY / VIEWPORT[3]) * resolution.getScaledHeight();
        return new double[] {screenX, screenY};
    }

    private static void setupOverlay(ScaledResolution resolution) {
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0.0D, resolution.getScaledWidth(), resolution.getScaledHeight(), 0.0D, -1.0D, 1.0D);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GlStateManager.disableDepth();
        GlStateManager.disableLighting();
    }

    private static void cleanupOverlay() {
        GlStateManager.enableDepth();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
    }
}
