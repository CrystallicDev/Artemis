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
import net.minecraft.client.network.NetworkPlayerInfo;
import net.minecraft.client.renderer.entity.RenderManager;
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

    private static final double MARKER_HEIGHT = 2.9D;
    private static final int ARROW_HEIGHT = 4;
    private static final int ARROW_THICKNESS = 1;
    private static final float ARROW_SLOPE = 1.0F;
    private static final double DISTANCE_TEXT_THRESHOLD = 50.0D;
    private static final double AIM_DOT_THRESHOLD = 0.99D;
    private static final int LABEL_ARGB = 0x78FFFFFF;

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
        RenderManager renderManager = mc.getRenderManager();
        Vec3 camera = new Vec3(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        ScaledResolution resolution = new ScaledResolution(mc);

        Vec3 eye = viewer.getPositionEyes(event.partialTicks);
        Vec3 look = viewer.getLook(event.partialTicks);
        List<Marker> markers = new ArrayList<>();
        for (TeamState.Member member : members) {
            if (member.uuid.equals(mc.thePlayer.getUniqueID())) {
                continue;
            }

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
                Vec3 direction = new Vec3(px - eye.xCoord, py + MARKER_HEIGHT - eye.yCoord,
                    pz - eye.zCoord).normalize();
                boolean aimed = look.dotProduct(direction) > AIM_DOT_THRESHOLD;
                markers.add(new Marker((int) screen[0], (int) screen[1], argb, distance,
                    aimed ? displayName(mc, member.uuid, entity) : null));
            }
        }

        if (markers.isEmpty()) {
            return;
        }

        setupOverlay(resolution);
        FontRenderer font = mc.fontRendererObj;
        for (Marker marker : markers) {
            drawArrow(marker.x, marker.y, marker.argb);
            if (marker.distance > DISTANCE_TEXT_THRESHOLD) {
                GlStateManager.enableBlend();
                GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
                String label = "(" + marker.distance + "m)";
                float labelY = marker.y + 2.0F;
                font.drawStringWithShadow(label, marker.x - font.getStringWidth(label) / 2.0F, labelY, LABEL_ARGB);
                if (marker.name != null) {
                    font.drawStringWithShadow(marker.name,
                        marker.x - font.getStringWidth(marker.name) / 2.0F,
                        labelY + font.FONT_HEIGHT + 1, LABEL_ARGB);
                }
                GlStateManager.disableBlend();
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

    private static final class Marker {
        final int x;
        final int y;
        final int argb;
        final int distance;
        final String name;

        Marker(int x, int y, int argb, int distance, String name) {
            this.x = x;
            this.y = y;
            this.argb = argb;
            this.distance = distance;
            this.name = name;
        }
    }

    private static String displayName(Minecraft mc, UUID uuid, EntityPlayer entity) {
        if (entity != null) {
            return entity.getDisplayName().getFormattedText();
        }
        if (mc.getNetHandler() == null) {
            return null;
        }
        NetworkPlayerInfo info = mc.getNetHandler().getPlayerInfo(uuid);
        return info == null ? null : mc.ingameGUI.getTabList().getPlayerName(info);
    }

    private static void drawArrow(int cx, int cy, int argb) {
        int top = cy - ARROW_HEIGHT;
        for (int row = 0; row < ARROW_HEIGHT; row++) {
            int offset = Math.round((ARROW_HEIGHT - 1 - row) * ARROW_SLOPE);
            int y = top + row;
            Gui.drawRect(cx - offset - ARROW_THICKNESS, y, cx - offset + ARROW_THICKNESS, y + 1, argb);
            Gui.drawRect(cx + offset - ARROW_THICKNESS, y, cx + offset + ARROW_THICKNESS, y + 1, argb);
        }
    }

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
