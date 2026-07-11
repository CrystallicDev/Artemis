package fr.natsu.artemis.module.chat;

import net.minecraft.client.gui.FontRenderer;

/**
 * Dessine une chaîne de chat en supportant les couleurs hex encodées au format 1.16
 * ({@code §x§r§r§g§g§b§b}) en plus des codes § classiques. Utilisé pour rendre les lignes du chat
 * vanilla (les messages normaux passent aussi par ici, rendus à l'identique).
 */
public final class HexTextRenderer {

    private static final char SECTION = '§';

    private static final int[] VANILLA_COLORS = {
        0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
        0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF
    };

    private HexTextRenderer() {
    }

    /** Construit le préfixe hex {@code §x§r§r§g§g§b§b} pour une couleur RGB. */
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
     * Dessine {@code text} avec ombre à partir de (x, y), en interprétant {@code §x} (hex) et les
     * codes § standards. Renvoie la position X finale.
     */
    public static int drawWithShadow(FontRenderer font, String text, float x, float y, int defaultColor) {
        return draw(font, text, x, y, defaultColor, true);
    }

    /**
     * Dessine {@code text} sans ombre à partir de (x, y), en interprétant {@code §x} (hex) et les
     * codes § standards. Renvoie la position X finale.
     */
    public static int draw(FontRenderer font, String text, float x, float y, int defaultColor) {
        return draw(font, text, x, y, defaultColor, false);
    }

    /**
     * Dessine {@code text} à partir de (x, y), en interprétant {@code §x} (hex) et les codes §
     * standards. {@code shadow} active l'ombre portée. Renvoie la position X finale.
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
                        i += 1 + 12; // saute §x + 6 paires
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

    /** Lit 6 paires {@code §<hexdigit>} à partir de {@code start}, ou {@code -1} si invalide. */
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
