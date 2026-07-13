package fr.natsu.artemis.module.chat;

import net.minecraft.client.gui.FontRenderer;

/**
 * Draws a string with support for 1.16-format hex colors ({@code §x§r§r§g§g§b§b}) on top of the
 * classic § codes. Called from the font-renderer hook, so normal text goes through here too and is
 * rendered identically.
 */
public final class HexTextRenderer {

    private static final char SECTION = '§';

    private static final int[] VANILLA_COLORS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private HexTextRenderer() {
    }

    /** Builds the {@code §x§r§r§g§g§b§b} hex prefix for an RGB color. */
    public static String hexPrefix(int rgb) {
        String hex = String.format("%06x", rgb & 0xFFFFFF);
        StringBuilder sb = new StringBuilder(14);
        sb.append(SECTION).append('x');
        for (int i = 0; i < 6; i++) {
            sb.append(SECTION).append(hex.charAt(i));
        }
        return sb.toString();
    }

    /**
     * Draws {@code text} with a shadow from (x, y), interpreting {@code §x} (hex) and the standard §
     * codes. Returns the final X position.
     */
    public static int drawWithShadow(FontRenderer font, String text, float x, float y, int defaultColor) {
        return draw(font, text, x, y, defaultColor, true);
    }

    /**
     * Draws {@code text} without a shadow from (x, y), interpreting {@code §x} (hex) and the standard §
     * codes. Returns the final X position.
     */
    public static int draw(FontRenderer font, String text, float x, float y, int defaultColor) {
        return draw(font, text, x, y, defaultColor, false);
    }

    /**
     * Draws {@code text} from (x, y), interpreting {@code §x} (hex) and the standard § codes.
     * {@code shadow} enables the drop shadow. Returns the final X position.
     */
    public static int draw(FontRenderer font, String text, float x, float y, int defaultColor, boolean shadow) {
        int alpha = (defaultColor >>> 24) & 0xFF;
        if (alpha == 0) {
            alpha = 0xFF;
        }
        int defaultRgb = defaultColor & 0xFFFFFF;
        int color = defaultRgb;
        boolean bold = false;
        boolean italic = false;
        boolean underline = false;
        boolean strike = false;
        boolean obfuscated = false;

        float cursorX = x;
        int length = text.length();
        for (int i = 0; i < length; i++) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < length) {
                char code = Character.toLowerCase(text.charAt(i + 1));

                if (code == 'x') {
                    int rgb = readHex(text, i + 2);
                    if (rgb >= 0) {
                        color = rgb;
                        bold = italic = underline = strike = obfuscated = false;
                        i += 1 + 12; // skip §x and the 6 pairs
                        continue;
                    }
                    i++;
                    continue;
                }

                int colorIndex = "0123456789abcdef".indexOf(code);
                if (colorIndex >= 0) {
                    color = VANILLA_COLORS[colorIndex];
                    bold = italic = underline = strike = obfuscated = false;
                } else {
                    switch (code) {
                        case 'l': bold = true; break;
                        case 'o': italic = true; break;
                        case 'n': underline = true; break;
                        case 'm': strike = true; break;
                        case 'k': obfuscated = true; break;
                        case 'r':
                            color = defaultRgb;
                            bold = italic = underline = strike = obfuscated = false;
                            break;
                        default: break;
                    }
                }
                i++;
                continue;
            }

            StringBuilder glyph = new StringBuilder(6);
            if (obfuscated) {
                glyph.append(SECTION).append('k');
            }
            if (bold) {
                glyph.append(SECTION).append('l');
            }
            if (strike) {
                glyph.append(SECTION).append('m');
            }
            if (underline) {
                glyph.append(SECTION).append('n');
            }
            if (italic) {
                glyph.append(SECTION).append('o');
            }
            glyph.append(c);

            int argb = (alpha << 24) | color;
            if (shadow) {
                font.drawStringWithShadow(glyph.toString(), cursorX, y, argb);
            } else {
                font.drawString(glyph.toString(), (int) cursorX, (int) y, argb);
            }
            cursorX += font.getCharWidth(c) + (bold ? 1 : 0);
        }
        return (int) cursorX;
    }

    /** Reads 6 {@code §<hexdigit>} pairs starting at {@code start}, or {@code -1} if invalid. */
    private static int readHex(String text, int start) {
        int rgb = 0;
        for (int k = 0; k < 6; k++) {
            int si = start + k * 2;
            if (si + 1 >= text.length() || text.charAt(si) != SECTION) {
                return -1;
            }
            int digit = Character.digit(text.charAt(si + 1), 16);
            if (digit < 0) {
                return -1;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }
}
