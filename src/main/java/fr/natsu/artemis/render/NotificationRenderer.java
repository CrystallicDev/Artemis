package fr.natsu.artemis.render;

import java.util.List;

import fr.natsu.artemis.module.nametag.AdventureText.Run;
import fr.natsu.artemis.module.notification.NotificationState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class NotificationRenderer {

    private static final int MARGIN = 5;
    private static final int PADDING = 4;
    private static final int ICON_SIZE = 16;
    private static final int GAP = 4;
    private static final int LINE_HEIGHT = 10;
    private static final int BG_ALPHA = 0xC0;
    private static final long FADE_IN_MS = 200L;
    private static final long FADE_OUT_MS = 400L;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Post event) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || NotificationState.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRendererObj;
        ScaledResolution resolution = new ScaledResolution(mc);
        int screenWidth = resolution.getScaledWidth();
        long now = System.currentTimeMillis();

        int y = MARGIN;
        for (NotificationState.Notification notification : NotificationState.all()) {
            long remaining = notification.remaining(now);
            if (remaining <= 0L) {
                NotificationState.remove(notification);
                continue;
            }
            float fade = fade(notification.elapsed(now), remaining);
            y = renderCard(mc, font, notification, screenWidth, y, fade) + GAP;
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static float fade(long elapsed, long remaining) {
        float in = elapsed >= FADE_IN_MS ? 1.0F : elapsed / (float) FADE_IN_MS;
        float out = remaining >= FADE_OUT_MS ? 1.0F : remaining / (float) FADE_OUT_MS;
        return Math.max(0.0F, Math.min(in, out));
    }

    private static int renderCard(Minecraft mc, FontRenderer font, NotificationState.Notification notification,
                                  int screenWidth, int y, float fade) {
        boolean hasIcon = notification.itemIcon != null || notification.textureIcon != null;
        int iconSpace = hasIcon ? ICON_SIZE + PADDING : 0;

        int textWidth = 0;
        for (List<Run> line : notification.titleLines) {
            textWidth = Math.max(textWidth, lineWidth(font, line));
        }
        for (List<Run> line : notification.descriptionLines) {
            textWidth = Math.max(textWidth, lineWidth(font, line));
        }
        int lineCount = notification.titleLines.size() + notification.descriptionLines.size();
        int textHeight = lineCount * LINE_HEIGHT;

        int cardWidth = PADDING + iconSpace + textWidth + PADDING;
        int cardHeight = Math.max(hasIcon ? ICON_SIZE : 0, textHeight) + PADDING * 2;
        int x = screenWidth - MARGIN - cardWidth;

        int alpha = (int) (255.0F * fade);
        int bgAlpha = (int) (BG_ALPHA * fade);
        Gui.drawRect(x, y, x + cardWidth, y + cardHeight, (bgAlpha << 24));

        if (hasIcon) {
            int iconY = y + (cardHeight - ICON_SIZE) / 2;
            drawIcon(mc, notification, x + PADDING, iconY, fade);
        }

        int textX = x + PADDING + iconSpace;
        int textY = y + (cardHeight - textHeight) / 2;
        for (List<Run> line : notification.titleLines) {
            drawLine(font, line, textX, textY, alpha);
            textY += LINE_HEIGHT;
        }
        for (List<Run> line : notification.descriptionLines) {
            drawLine(font, line, textX, textY, alpha);
            textY += LINE_HEIGHT;
        }
        return y + cardHeight;
    }

    private static void drawIcon(Minecraft mc, NotificationState.Notification notification, int x, int y, float fade) {
        if (notification.itemIcon != null) {
            RenderItem renderItem = mc.getRenderItem();
            GlStateManager.enableRescaleNormal();
            RenderHelper.enableGUIStandardItemLighting();
            renderItem.renderItemIntoGUI(notification.itemIcon, x, y);
            RenderHelper.disableStandardItemLighting();
            GlStateManager.disableRescaleNormal();
        } else if (notification.textureIcon != null) {
            mc.getTextureManager().bindTexture(notification.textureIcon);
            GlStateManager.enableBlend();
            GlStateManager.color(1.0F, 1.0F, 1.0F, fade);
            Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, 16, 16, ICON_SIZE, ICON_SIZE, 16.0F, 16.0F);
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        }
    }

    private static int lineWidth(FontRenderer font, List<Run> runs) {
        int width = 0;
        for (Run run : runs) {
            width += font.getStringWidth(run.display);
        }
        return width;
    }

    private static void drawLine(FontRenderer font, List<Run> runs, int x, int y, int alpha) {
        int cursor = x;
        for (Run run : runs) {
            int color = (alpha << 24) | (run.rgb & 0xFFFFFF);
            font.drawStringWithShadow(run.display, cursor, y, color);
            cursor += font.getStringWidth(run.display);
        }
    }
}
