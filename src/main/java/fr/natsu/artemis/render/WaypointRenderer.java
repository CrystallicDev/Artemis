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
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Rend les waypoints Apollo : un beam vertical coloré (beacon) au bloc, et un label (nom + distance)
 * projeté à l'écran, clampé au bord quand le waypoint est hors champ.
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

        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(mc.thePlayer, event.partialTicks);
        ScaledResolution resolution = new ScaledResolution(mc);

        // Passe 1 (matrices monde 3D) : beams + projection des labels.
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

        // Passe 2 (2D) : labels nom + distance.
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

    /** Beam vertical (deux quads croisés) au bloc, visible à travers les blocs. */
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
        // Quad le long de X.
        worldRenderer.pos(-BEAM_HALF_WIDTH, 0.0D, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(BEAM_HALF_WIDTH, 0.0D, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(BEAM_HALF_WIDTH, BEAM_HEIGHT, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(-BEAM_HALF_WIDTH, BEAM_HEIGHT, 0.0D).color(r, g, b, BEAM_ALPHA).endVertex();
        // Quad le long de Z.
        worldRenderer.pos(0.0D, 0.0D, -BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, 0.0D, BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, BEAM_HEIGHT, BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        worldRenderer.pos(0.0D, BEAM_HEIGHT, -BEAM_HALF_WIDTH).color(r, g, b, BEAM_ALPHA).endVertex();
        tessellator.draw();

        GlStateManager.enableDepth();
        GlStateManager.depthMask(true);
        GlStateManager.enableCull();
        // NE PAS ré-activer le lighting : il est déjà désactivé au RenderWorldLastEvent. Le rallumer le
        // laisse actif jusqu'au rendu 2D (HUD/inventaire) -> texte grisé et glint d'enchantement noir.
        // Ce bug n'apparaissait que waypoint hors écran : sinon la passe overlay des labels laissait le
        // lighting désactivé et masquait la fuite.
        // Blend func ADDITIF (SRC_ALPHA, ONE) posé plus haut : le remettre au défaut avant de couper le
        // blend, sinon il resterait dans le cache GlStateManager et délaverait le texte du HUD.
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    /** Contour filaire (wireframe) du bloc du waypoint, visible à travers les blocs. */
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
        // 12 arêtes du cube unité (0..1).
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
        // On ne ré-active PAS le lighting (désactivé au RenderWorldLastEvent) : le laisser actif noircit
        // le rendu 2D suivant (glint d'enchantement noir, texte du HUD grisé).
        GlStateManager.disableBlend();
        GlStateManager.enableTexture2D();
        GL11.glLineWidth(1.0F); // ne pas laisser fuiter l'épaisseur de trait vers d'autres rendus (F3...)
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }

    private static void edge(WorldRenderer wr, double x1, double y1, double z1,
                             double x2, double y2, double z2, float r, float g, float b) {
        wr.pos(x1, y1, z1).color(r, g, b, 1.0F).endVertex();
        wr.pos(x2, y2, z2).color(r, g, b, 1.0F).endVertex();
    }

    // Buffers/matrices réutilisés (thread de rendu unique) pour éviter des allocations par waypoint.
    private static final FloatBuffer MODELVIEW_BUF = BufferUtils.createFloatBuffer(16);
    private static final FloatBuffer PROJECTION_BUF = BufferUtils.createFloatBuffer(16);
    private static final IntBuffer VIEWPORT_BUF = BufferUtils.createIntBuffer(16);
    private static final float[] MODELVIEW = new float[16];
    private static final float[] PROJECTION = new float[16];
    private static final int[] VIEWPORT = new int[4];

    /**
     * Projette un point (relatif à la caméra) en coordonnées écran scaled. Retourne {@code null}
     * UNIQUEMENT si le point est <b>derrière</b> la caméra (via le signe de {@code w} en clip space).
     *
     * <p>Contrairement à {@code gluProject}, on n'utilise PAS la profondeur ({@code winZ}) : un point
     * au-delà du far plane (waypoint très lointain) se projette quand même correctement en X/Y, il ne
     * faut donc pas le rejeter. Le clamp au bord de l'écran est fait par l'appelant.</p>
     */
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

        // eye = ModelView * (x, y, z, 1)
        float ex = MODELVIEW[0] * (float) x + MODELVIEW[4] * (float) y + MODELVIEW[8] * (float) z + MODELVIEW[12];
        float ey = MODELVIEW[1] * (float) x + MODELVIEW[5] * (float) y + MODELVIEW[9] * (float) z + MODELVIEW[13];
        float ez = MODELVIEW[2] * (float) x + MODELVIEW[6] * (float) y + MODELVIEW[10] * (float) z + MODELVIEW[14];
        float ew = MODELVIEW[3] * (float) x + MODELVIEW[7] * (float) y + MODELVIEW[11] * (float) z + MODELVIEW[15];

        // clip = Projection * eye ; seuls X, Y et W nous intéressent (pas la profondeur Z).
        float cx = PROJECTION[0] * ex + PROJECTION[4] * ey + PROJECTION[8] * ez + PROJECTION[12] * ew;
        float cy = PROJECTION[1] * ex + PROJECTION[5] * ey + PROJECTION[9] * ez + PROJECTION[13] * ew;
        float cw = PROJECTION[3] * ex + PROJECTION[7] * ey + PROJECTION[11] * ez + PROJECTION[15] * ew;

        if (cw <= 1.0E-4F) {
            return null; // derrière la caméra (ou dessus) : coordonnées non fiables -> on n'affiche pas
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
