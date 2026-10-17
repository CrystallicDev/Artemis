package fr.natsu.artemis.module.icon;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;

public final class IconTextRenderer {

    private static final char TOKEN = ':';
    private static final int MAX_NAME = 32;

    private static final class Part {
        final String text;
        final IconRegistry.Icon icon;

        Part(String text, IconRegistry.Icon icon) {
            this.text = text;
            this.icon = icon;
        }
    }

    private IconTextRenderer() {
    }

    public static boolean contains(String text) {
        if (text == null || IconRegistry.isEmpty()) {
            return false;
        }
        int start = text.indexOf(TOKEN);
        while (start >= 0) {
            int end = findClosing(text, start);
            if (end > 0 && IconRegistry.get(text.substring(start + 1, end)) != null) {
                return true;
            }
            start = text.indexOf(TOKEN, start + 1);
        }
        return false;
    }

    public static int draw(FontRenderer font, String text, float x, float y, int color, boolean shadow) {
        return draw(font, text, x, y, color, shadow, false);
    }

    public static int draw(FontRenderer font, String text, float x, float y, int color, boolean shadow,
                           boolean shadowPass) {
        if (shadowPass) {
            color = (color & 0xFCFCFC) >> 2 | color & 0xFF000000;
        }
        if ((color & 0xFC000000) == 0) {
            color |= 0xFF000000;
        }
        int alpha = (color >>> 24) & 0xFF;

        List<Part> parts = split(text);
        StringBuilder seen = new StringBuilder();
        float cursor = x;

        for (Part part : parts) {
            if (part.icon != null) {
                if (!shadowPass) {
                    drawIcon(part.icon, cursor, y, alpha);
                }
                cursor += part.icon.width;
                continue;
            }
            String segment = seen.length() == 0 ? part.text
                : FontRenderer.getFormatFromString(seen.toString()) + part.text;
            cursor = font.drawString(segment, cursor, y, color, shadow);
            seen.append(part.text);
        }
        return (int) cursor;
    }

    public static int width(FontRenderer font, String text) {
        List<Part> parts = split(text);
        StringBuilder seen = new StringBuilder();
        float total = 0.0F;

        for (Part part : parts) {
            if (part.icon != null) {
                total += part.icon.width;
                continue;
            }
            String segment = seen.length() == 0 ? part.text
                : FontRenderer.getFormatFromString(seen.toString()) + part.text;
            total += font.getStringWidth(segment);
            seen.append(part.text);
        }
        return (int) total;
    }

    private static List<Part> split(String text) {
        List<Part> parts = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        int i = 0;

        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == TOKEN) {
                int end = findClosing(text, i);
                if (end > 0) {
                    IconRegistry.Icon icon = IconRegistry.get(text.substring(i + 1, end));
                    if (icon != null) {
                        if (buffer.length() > 0) {
                            parts.add(new Part(buffer.toString(), null));
                            buffer.setLength(0);
                        }
                        parts.add(new Part(null, icon));
                        i = end + 1;
                        continue;
                    }
                }
            }
            buffer.append(c);
            i++;
        }
        if (buffer.length() > 0) {
            parts.add(new Part(buffer.toString(), null));
        }
        return parts;
    }

    private static int findClosing(String text, int start) {
        int limit = Math.min(text.length(), start + MAX_NAME + 2);
        for (int i = start + 1; i < limit; i++) {
            char c = text.charAt(i);
            if (c == TOKEN) {
                return i > start + 1 ? i : -1;
            }
            if (!isNameChar(c)) {
                return -1;
            }
        }
        return -1;
    }

    private static boolean isNameChar(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')
            || (c >= '0' && c <= '9') || c == '_' || c == '-' || c == '.';
    }

    private static void drawIcon(IconRegistry.Icon icon, float x, float y, int alpha) {
        Minecraft.getMinecraft().getTextureManager().bindTexture(icon.texture);

        GlStateManager.enableBlend();
        GlStateManager.tryBlendFuncSeparate(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
            GL11.GL_ONE, GL11.GL_ZERO);
        GlStateManager.color(1.0F, 1.0F, 1.0F, alpha / 255.0F);

        float top = y + icon.ascent;
        float bottom = top + icon.height;
        float right = x + icon.width;

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX);
        worldRenderer.pos(x, bottom, 0.0D).tex(0.0D, 1.0D).endVertex();
        worldRenderer.pos(right, bottom, 0.0D).tex(1.0D, 1.0D).endVertex();
        worldRenderer.pos(right, top, 0.0D).tex(1.0D, 0.0D).endVertex();
        worldRenderer.pos(x, top, 0.0D).tex(0.0D, 0.0D).endVertex();
        tessellator.draw();

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
