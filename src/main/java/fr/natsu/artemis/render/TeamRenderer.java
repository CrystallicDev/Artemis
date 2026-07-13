package fr.natsu.artemis.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.util.UUID;

import fr.natsu.artemis.module.team.TeamState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the TeamView markers Lunar-style: a small arrow (a caret pointing down) in the
 * {@code markerColor}, above each tagged teammate's head, visible through blocks.
 *
 * <p>The arrow is drawn with {@link Gui#drawRect} (the most reliable vanilla 2D primitive: it keeps
 * its own GL texture/blend state consistent), after a {@code gluProject} projection.</p>
 */
public final class TeamRenderer {

    private static final double MARKER_HEIGHT = 4.5D;
    private static final int ARROW_HEIGHT = 4;
    private static final int ARROW_THICKNESS = 1;
    private static final float ARROW_SLOPE = 1.0F;
    /** The distance ({@code <n>m}) only shows under the arrow past this threshold (like Lunar). */
    private static final double DISTANCE_TEXT_THRESHOLD = 50.0D;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        List<TeamState.Member> members = TeamState.members();
        if (members.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.getRenderViewEntity() == null) {
            return;
        }

        Entity viewer = mc.getRenderViewEntity();
        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(mc.thePlayer, event.partialTicks);
        ScaledResolution resolution = new ScaledResolution(mc);

        // Pass 1: project WHILE the 3D world matrices are still active.
        List<int[]> markers = new ArrayList<>();
        for (TeamState.Member member : members) {
            if (member.uuid.equals(mc.thePlayer.getUniqueID())) {
                continue;
            }

            // Position: interpolated entity if the player is loaded (smooth, in sync with their
            // rendering), otherwise the Apollo position (player out of range / through walls).
            double px = member.x;
            double py = member.y;
            double pz = member.z;
            EntityPlayer entity = findPlayer(mc, member.uuid);
            if (entity != null) {
                float pt = event.partialTicks;
                px = entity.lastTickPosX + (entity.posX - entity.lastTickPosX) * pt;
                py = entity.lastTickPosY + (entity.posY - entity.lastTickPosY) * pt;
                pz = entity.lastTickPosZ + (entity.posZ - entity.lastTickPosZ) * pt;
            }

            double[] screen = project(
                px - camera.xCoord,
                py + MARKER_HEIGHT - camera.yCoord,
                pz - camera.zCoord,
                resolution);
            if (screen != null) {
                int argb = member.markerArgb;
                if ((argb >>> 24) == 0) {
                    argb |= 0xFF000000;
                }
                int distance = (int) Math.round(viewer.getDistance(px, py, pz));
                markers.add(new int[] {(int) screen[0], (int) screen[1], argb, distance});
            }
        }

        if (markers.isEmpty()) {
            return;
        }

        // Pass 2: draw in 2D.
        setupOverlay(resolution);
        FontRenderer font = mc.fontRendererObj;
        for (int[] marker : markers) {
            drawArrow(marker[0], marker[1], marker[2]);
            if (marker[3] > DISTANCE_TEXT_THRESHOLD) {
                String label = "(" + marker[3] + "m)";
                font.drawStringWithShadow(label, marker[0] - font.getStringWidth(label) / 2.0F,
                    marker[1] + 2.0F, 0xFFFFFFFF);
            }
        }
        cleanupOverlay();
    }

    private static EntityPlayer findPlayer(Minecraft mc, UUID uuid) {
        if (mc.theWorld == null) {
            return null;
        }
        for (EntityPlayer player : mc.theWorld.playerEntities) {
            if (player.getUniqueID().equals(uuid)) {
                return player;
            }
        }
        return null;
    }

    /** A downward V chevron: two arms converging at the bottom point, via {@link Gui#drawRect}. */
    private static void drawArrow(int cx, int cy, int argb) {
        int top = cy - ARROW_HEIGHT;
        for (int row = 0; row < ARROW_HEIGHT; row++) {
            // Arms spread out at the top, meeting at the bottom point (toward the head).
            int offset = Math.round((ARROW_HEIGHT - 1 - row) * ARROW_SLOPE);
            int y = top + row;
            // Left arm.
            Gui.drawRect(cx - offset - ARROW_THICKNESS, y, cx - offset + ARROW_THICKNESS, y + 1, argb);
            // Right arm.
            Gui.drawRect(cx + offset - ARROW_THICKNESS, y, cx + offset + ARROW_THICKNESS, y + 1, argb);
        }
    }

    /** Projects a position (relative to the camera) to "scaled" screen coords, or {@code null} if behind. */
    private static double[] project(double x, double y, double z, ScaledResolution resolution) {
        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);
        FloatBuffer win = BufferUtils.createFloatBuffer(3);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        if (!GLU.gluProject((float) x, (float) y, (float) z, modelView, projection, viewport, win)) {
            return null;
        }
        float winZ = win.get(2);
        if (winZ < 0.0F || winZ > 1.0F) {
            return null;
        }

        double screenX = (win.get(0) / viewport.get(2)) * resolution.getScaledWidth();
        double screenY = resolution.getScaledHeight() - (win.get(1) / viewport.get(3)) * resolution.getScaledHeight();
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
