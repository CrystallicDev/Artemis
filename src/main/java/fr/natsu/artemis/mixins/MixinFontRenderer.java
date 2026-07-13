package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.module.chat.HexTextRenderer;
import net.minecraft.client.gui.FontRenderer;

/**
 * Native support for hex colors ({@code §x§r§r§g§g§b§b}) in the {@code FontRenderer}, so they work
 * everywhere text is drawn (tab, nametag, sidebar, chat, items, holograms...) whatever the caller is,
 * including OptiFine and third-party mods like OldAnimations that redraw the tab in 1.7.10 style. This
 * is the one universal choke point: every public draw method
 * ({@code drawString}/{@code drawStringWithShadow}) funnels into
 * {@code drawString(String, float, float, int, boolean)}.
 *
 * <ul>
 *   <li>{@link #artemis$hexDraw}: intercepts that funnel; if the string contains {@code §x} it is
 *       drawn by {@link HexTextRenderer} (RGB color resolved per glyph) and the vanilla render is
 *       cancelled. Otherwise no impact (vanilla/OptiFine handles it). No recursion: the glyphs
 *       redrawn by {@link HexTextRenderer} no longer contain {@code §x}.</li>
 *   <li>{@link #artemis$hexFormat}: keeps {@code §x} alive across chat line wrapping, whose wrapping
 *       carries the formatting over via {@code getFormatFromString} (which doesn't know {@code §x}).</li>
 * </ul>
 */
@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    private static final char SECTION = '§';

    @Inject(
        method = "drawString(Ljava/lang/String;FFIZ)I",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$hexDraw(String text, float x, float y, int color, boolean dropShadow,
            CallbackInfoReturnable<Integer> cir) {
        if (text == null || !containsHex(text)) {
            return; // no hex: let vanilla/OptiFine draw normally
        }
        FontRenderer self = (FontRenderer) (Object) this;
        cir.setReturnValue(HexTextRenderer.draw(self, text, x, y, color, dropShadow));
    }

    /** True if the string contains a hex {@code §x} sequence (case-insensitive on the {@code x}). */
    private static boolean containsHex(String text) {
        int i = -1;
        while ((i = text.indexOf(SECTION, i + 1)) != -1 && i + 1 < text.length()) {
            if (Character.toLowerCase(text.charAt(i + 1)) == 'x') {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "getFormatFromString", at = @At("HEAD"), cancellable = true, require = 0)
    private static void artemis$hexFormat(String text, CallbackInfoReturnable<String> cir) {
        if (text == null || text.indexOf(SECTION + "x") < 0) {
            return;
        }
        cir.setReturnValue(computeFormat(text));
    }

    /** Like vanilla {@code getFormatFromString} but keeping the whole {@code §x} sequences intact. */
    private static String computeFormat(String text) {
        StringBuilder format = new StringBuilder();
        int length = text.length();
        int i = -1;
        while ((i = text.indexOf(SECTION, i + 1)) != -1) {
            if (i >= length - 1) {
                break;
            }
            char code = text.charAt(i + 1);
            if (Character.toLowerCase(code) == 'x' && i + 13 < length) {
                // §x + 6 §<hexdigit> pairs: a color, which resets the accumulated format.
                format.setLength(0);
                format.append(text, i, i + 14);
                i += 13;
            } else if (isColor(code)) {
                format.setLength(0);
                format.append(SECTION).append(code);
            } else if (isSpecial(code)) {
                format.append(SECTION).append(code);
            }
        }
        return format.toString();
    }

    private static boolean isColor(char c) {
        char l = Character.toLowerCase(c);
        return (l >= '0' && l <= '9') || (l >= 'a' && l <= 'f');
    }

    private static boolean isSpecial(char c) {
        char l = Character.toLowerCase(c);
        return (l >= 'k' && l <= 'o') || l == 'r';
    }
}
