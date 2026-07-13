package fr.natsu.artemis.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.ArtemisConfig;
import fr.natsu.artemis.module.cooldown.CooldownState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the Apollo cooldowns in the HUD, Lunar Client style: a "track" ring ({@code circleEnd})
 * covered by a progress arc ({@code circleStart}) proportional to the remaining time (which empties
 * out as the animation), edge rings ({@code circleEdge}), the item icon (with glint) in the center,
 * and the remaining time as text ({@code textColor}).
 *
 * <p>Layout and proportions taken from Lunar's cooldown renderer: ~34px cell, item at 1.4×, ring
 * ~11.5 to 15.</p>
 */
public final class CooldownRenderer {

    private static final int SLOT_SPACING = 34;
    private static final float RADIUS_OUTER = 15.0F;
    private static final float RADIUS_INNER = 11.5F;
    private static final float ITEM_SCALE = 1.4F;
    private static final int SEGMENTS = 48;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL) {
            return;
        }

        List<CooldownState.Entry> cooldowns = CooldownState.active();
        if (cooldowns.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        int anchorX = Math.round(ArtemisConfig.cooldownX * resolution.getScaledWidth());
        int anchorY = Math.round(ArtemisConfig.cooldownY * resolution.getScaledHeight());

        for (int i = 0; i < cooldowns.size(); i++) {
            int[] pos = slotPosition(anchorX, anchorY, i);
            renderCooldown(mc, cooldowns.get(i), pos[0], pos[1]);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    /** Position of the i-th cooldown from the anchor, in the configured direction. */
    public static int[] slotPosition(int anchorX, int anchorY, int index) {
        int offset = index * SLOT_SPACING;
        switch (ArtemisConfig.cooldownDirection) {
            case LEFT:
                return new int[] {anchorX - offset, anchorY};
            case DOWN:
                return new int[] {anchorX, anchorY + offset};
            case UP:
                return new int[] {anchorX, anchorY - offset};
            case RIGHT:
            default:
                return new int[] {anchorX + offset, anchorY};
        }
    }

    /** Preview of a fake cooldown centered at (cx, cy), for the config screen. */
    public static void renderPreview(int cx, int cy) {
        int start = 0x99595959;
        int end = 0xFFE5E5E5;
        int edge = 0xFF000000;
        float fraction = 0.6F;
        fillArc(cx, cy, RADIUS_OUTER, 0.0F, 0.0F, 360.0F, 0x55000000);
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_INNER, 0.0F, 360.0F, end);
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_INNER, -90.0F, 360.0F * fraction, start);
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_OUTER - 0.8F, 0.0F, 360.0F, edge);
        fillArc(cx, cy, RADIUS_INNER + 0.8F, RADIUS_INNER, 0.0F, 360.0F, edge);
        FontRenderer font = Minecraft.getMinecraft().fontRendererObj;
        String label = "3.0";
        font.drawStringWithShadow(label, cx - font.getStringWidth(label) / 2.0F, cy + RADIUS_OUTER + 2.0F, 0xFFFFFFFF);
    }

    private void renderCooldown(Minecraft mc, CooldownState.Entry entry, float cx, float cy) {
        float fraction = entry.remainingFraction();

        // Subtle dark backdrop under the ring.
        fillArc(cx, cy, RADIUS_OUTER, 0.0F, 0.0F, 360.0F, 0x55000000);
        // Full track (the "empty" color).
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_INNER, 0.0F, 360.0F, entry.circleEndArgb);
        // Progress arc (the "full" color), swept from the top, proportional to what remains.
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_INNER, -90.0F, 360.0F * fraction, entry.circleStartArgb);
        // Thin edge rings (inner + outer).
        fillArc(cx, cy, RADIUS_OUTER, RADIUS_OUTER - 0.8F, 0.0F, 360.0F, entry.circleEdgeArgb);
        fillArc(cx, cy, RADIUS_INNER + 0.8F, RADIUS_INNER, 0.0F, 360.0F, entry.circleEdgeArgb);

        // Item icon (with glint), scaled and centered.
        if (entry.icon != null) {
            RenderItem renderItem = mc.getRenderItem();
            GlStateManager.pushMatrix();
            GlStateManager.translate(cx, cy, 0.0F);
            GlStateManager.scale(ITEM_SCALE, ITEM_SCALE, 1.0F);
            GlStateManager.enableRescaleNormal();
            GlStateManager.enableBlend();
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemAndEffectIntoGUI(entry.icon, -8, -8);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
            GlStateManager.popMatrix();
        }

        // Remaining time as text, under the ring.
        FontRenderer font = mc.fontRendererObj;
        String label = formatRemaining(entry.remainingMillis());
        float textX = cx - font.getStringWidth(label) / 2.0F;
        font.drawStringWithShadow(label, textX, cy + RADIUS_OUTER + 2.0F, entry.textArgb);
    }

    private static String formatRemaining(long millis) {
        if (millis >= 3_600_000L) {
            return (millis / 3_600_000L) + "h" + ((millis % 3_600_000L) / 60_000L) + "m";
        }
        if (millis >= 60_000L) {
            return (millis / 60_000L) + "m" + ((millis % 60_000L) / 1_000L) + "s";
        }
        float seconds = millis / 1000.0F;
        if (seconds >= 10.0F) {
            return Math.round(seconds) + "s";
        }
        return String.format("%.1f", seconds);
    }

    /**
     * Draws a ring sector (or a disc if {@code rInner=0}) between two radii, from {@code startDeg}
     * over {@code sweepDeg} degrees (clockwise from the top if start=-90).
     */
    private static void fillArc(float cx, float cy, float rOuter, float rInner,
                                float startDeg, float sweepDeg, int argb) {
        if (sweepDeg <= 0.0F) {
            return;
        }
        int alpha = (argb >>> 24) & 0xFF;
        float a = alpha == 0 ? 1.0F : alpha / 255.0F;
        float r = ((argb >> 16) & 0xFF) / 255.0F;
        float g = ((argb >> 8) & 0xFF) / 255.0F;
        float b = (argb & 0xFF) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GL11.glColor4f(r, g, b, a);

        int steps = Math.max(1, Math.round(SEGMENTS * (sweepDeg / 360.0F)));
        GL11.glBegin(GL11.GL_TRIANGLE_STRIP);
        for (int i = 0; i <= steps; i++) {
            double angle = Math.toRadians(startDeg + sweepDeg * (i / (double) steps));
            float cos = (float) Math.cos(angle);
            float sin = (float) Math.sin(angle);
            GL11.glVertex2f(cx + cos * rOuter, cy + sin * rOuter);
            GL11.glVertex2f(cx + cos * rInner, cy + sin * rInner);
        }
        GL11.glEnd();

        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
