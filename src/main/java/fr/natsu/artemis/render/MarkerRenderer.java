package fr.natsu.artemis.render;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import fr.natsu.artemis.module.marker.MarkerState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.AbstractClientPlayer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.ActiveRenderInfo;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Rend les markers Apollo de façon complète : une icône issue du {@code target} (item, tête de joueur,
 * ou repère générique) sur un fond coloré par le {@code flag}, avec des lignes d'info (description,
 * propriétaire, coordonnées, distance) dont l'affichage suit un {@code MarkerDisplayCondition}
 * (notamment {@code HOVER} = affiché seulement quand le marker est visé).
 */
public final class MarkerRenderer {

    private static final double MARKER_HEIGHT = 0.5D;
    private static final float HOVER_RADIUS = 28.0F;
    private static final int LINE_HEIGHT = 10;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (MarkerState.all().isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        Entity viewer = mc.getRenderViewEntity();
        if (viewer == null) {
            return;
        }

        Vec3 camera = ActiveRenderInfo.projectViewFromEntity(mc.thePlayer, event.partialTicks);
        ScaledResolution resolution = new ScaledResolution(mc);
        float centerX = resolution.getScaledWidth() / 2.0F;
        float centerY = resolution.getScaledHeight() / 2.0F;

        List<Object[]> visible = new ArrayList<>();
        for (MarkerState.Marker marker : MarkerState.all()) {
            double[] screen = project(
                marker.x - camera.xCoord,
                marker.y + MARKER_HEIGHT - camera.yCoord,
                marker.z - camera.zCoord,
                resolution);
            if (screen == null) {
                continue;
            }
            int distance = (int) Math.round(viewer.getDistance(marker.x, marker.y, marker.z));
            boolean hovered = Math.hypot(screen[0] - centerX, screen[1] - centerY) < HOVER_RADIUS;
            visible.add(new Object[] {(int) screen[0], (int) screen[1], marker, distance, hovered});
        }

        if (visible.isEmpty()) {
            return;
        }

        setupOverlay(resolution);
        FontRenderer font = mc.fontRendererObj;
        for (Object[] entry : visible) {
            drawMarker(mc, font, (int) entry[0], (int) entry[1], (MarkerState.Marker) entry[2],
                (Integer) entry[3], (Boolean) entry[4]);
        }
        cleanupOverlay();
    }

    private static void drawMarker(Minecraft mc, FontRenderer font, int x, int y, MarkerState.Marker marker,
                                   int distance, boolean hovered) {
        int radius = Math.max(4, Math.round(5 * marker.scale));

        // Le repère permanent : un petit losange à la couleur du flag (contour sombre).
        drawDiamond(x, y, radius + 1, 0xFF000000);
        drawDiamond(x, y, radius, opaque(marker.iconArgb));

        // Lignes d'info conditionnelles, sous le losange. Tête/item = inline, taille du texte.
        int lineY = y + radius + 4;
        if (shouldShow(marker.showDescription, hovered)) {
            lineY = drawDescriptionLine(mc, font, marker, x, lineY);
        }
        if (shouldShow(marker.showOwner, hovered) && !marker.ownerName.isEmpty()) {
            boolean head = marker.ownerDisplay == MarkerState.OWNER_HEAD;
            lineY = drawHeadNameLine(mc, font, head, marker.ownerName, marker.ownerName, x, lineY,
                0xFFFFFFFF, marker.textShadow);
        }
        if (shouldShow(marker.showCoordinates, hovered)) {
            String coords = (int) marker.x + ", " + (int) marker.y + ", " + (int) marker.z;
            lineY = drawLine(font, coords, x, lineY, 0xFFAAAAAA, marker.textShadow);
        }
        if (shouldShow(marker.showDistance, hovered)) {
            drawLine(font, "(" + distance + "m)", x, lineY, 0xFFFFFFFF, marker.textShadow);
        }
    }

    /** Losange plein (rangées empilées de {@link Gui#drawRect}), largeur maximale au centre. */
    private static void drawDiamond(int cx, int cy, int radius, int argb) {
        for (int dy = -radius; dy <= radius; dy++) {
            int halfWidth = radius - Math.abs(dy);
            Gui.drawRect(cx - halfWidth, cy + dy, cx + halfWidth + 1, cy + dy + 1, argb);
        }
    }

    /** Ligne de description : selon le target, une tête+nom (Player), un item+id (Item/Block) ou l'id. */
    private static int drawDescriptionLine(Minecraft mc, FontRenderer font, MarkerState.Marker marker, int x, int y) {
        MarkerState.Target target = marker.target;
        int color = opaque(marker.iconArgb);
        switch (target.type) {
            case MarkerState.TARGET_PLAYER: {
                String name = target.playerName != null ? target.playerName : marker.id;
                return drawHeadNameLine(mc, font, true, target.playerName, name, x, y, color, marker.textShadow);
            }
            case MarkerState.TARGET_ITEM:
            case MarkerState.TARGET_BLOCK:
                if (target.item != null) {
                    return drawItemNameLine(mc, font, target.item, marker.id, x, y, color, marker.textShadow);
                }
                break;
            default:
                break;
        }
        return drawLine(font, marker.id, x, y, color, marker.textShadow);
    }

    /** Ligne « [petite tête] texte », inline, à la taille du texte. */
    private static int drawHeadNameLine(Minecraft mc, FontRenderer font, boolean showHead, String skinName,
                                        String text, int x, int y, int color, boolean shadow) {
        if (!showHead) {
            return drawLine(font, text, x, y, color, shadow);
        }
        int icon = font.FONT_HEIGHT;
        int total = icon + 2 + font.getStringWidth(text);
        int startX = x - total / 2;
        drawPlayerHead(mc, skinName, startX, y, icon);
        drawText(font, text, startX + icon + 2, y, color, shadow);
        return y + LINE_HEIGHT;
    }

    /** Ligne « [petite icône item] texte », inline. */
    private static int drawItemNameLine(Minecraft mc, FontRenderer font, net.minecraft.item.ItemStack stack,
                                        String text, int x, int y, int color, boolean shadow) {
        int icon = font.FONT_HEIGHT + 2;
        int total = icon + 2 + font.getStringWidth(text);
        int startX = x - total / 2;
        drawItem(mc, stack, startX, y - 1, icon);
        drawText(font, text, startX + icon + 2, y, color, shadow);
        return y + LINE_HEIGHT;
    }

    /** Item mis à l'échelle, coin haut-gauche en (x, y). */
    private static void drawItem(Minecraft mc, net.minecraft.item.ItemStack stack, int x, int y, int size) {
        RenderItem renderItem = mc.getRenderItem();
        float scale = size / 16.0F;
        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, 0.0F);
        GlStateManager.scale(scale, scale, 1.0F);
        GlStateManager.enableDepth();
        GlStateManager.enableRescaleNormal();
        RenderHelper.enableGUIStandardItemLighting();
        renderItem.renderItemIntoGUI(stack, 0, 0);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableRescaleNormal();
        GlStateManager.disableDepth();
        GlStateManager.popMatrix();
    }

    /** Tête de joueur (visage + chapeau), coin haut-gauche en (x, y). */
    private static void drawPlayerHead(Minecraft mc, String name, int x, int y, int size) {
        ResourceLocation skin = AbstractClientPlayer.getLocationSkin(name == null ? "" : name);
        mc.getTextureManager().bindTexture(skin);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.enableBlend();
        Gui.drawScaledCustomSizeModalRect(x, y, 8.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
        Gui.drawScaledCustomSizeModalRect(x, y, 40.0F, 8.0F, 8, 8, size, size, 64.0F, 64.0F);
    }

    private static void drawText(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        if (shadow) {
            font.drawStringWithShadow(text, x, y, color);
        } else {
            font.drawString(text, x, y, color);
        }
    }

    private static int drawLine(FontRenderer font, String text, int x, int y, int color, boolean shadow) {
        float lineX = x - font.getStringWidth(text) / 2.0F;
        if (shadow) {
            font.drawStringWithShadow(text, lineX, y, color);
        } else {
            font.drawString(text, (int) lineX, y, color);
        }
        return y + LINE_HEIGHT;
    }

    /** ALWAYS -> toujours ; HOVER (et UNSPECIFIED) -> seulement si visé ; NEVER -> jamais. */
    private static boolean shouldShow(int condition, boolean hovered) {
        if (condition == MarkerState.COND_ALWAYS) {
            return true;
        }
        if (condition == MarkerState.COND_NEVER) {
            return false;
        }
        return hovered;
    }

    private static int opaque(int argb) {
        return 0xFF000000 | (argb & 0xFFFFFF);
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
