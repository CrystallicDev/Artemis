package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.module.chat.HexTextRenderer;
import net.minecraft.client.gui.FontRenderer;

/**
 * Support natif des couleurs hex ({@code §x§r§r§g§g§b§b}) dans le {@code FontRenderer}, donc partout
 * où du texte est dessiné (tab, nametag, sidebar, chat, items, hologrammes...) quel que soit
 * l'appelant — y compris OptiFine et des mods tiers comme OldAnimations qui redessinent le tab en
 * 1.7.10. C'est le seul point de passage universel : toutes les méthodes de dessin publiques
 * ({@code drawString}/{@code drawStringWithShadow}) convergent vers
 * {@code drawString(String, float, float, int, boolean)}.
 *
 * <ul>
 *   <li>{@link #artemis$hexDraw} : intercepte ce funnel ; si la chaîne contient {@code §x}, elle est
 *       rendue par {@link HexTextRenderer} (couleur RGB résolue par glyphe) et le rendu vanilla est
 *       annulé. Sinon aucun impact (vanilla/OptiFine gère). Pas de récursion : les glyphes redessinés
 *       par {@link HexTextRenderer} ne contiennent plus de {@code §x}.</li>
 *   <li>{@link #artemis$hexFormat} : fait survivre {@code §x} au retour à la ligne du chat, dont le
 *       wrapping reporte le formatage via {@code getFormatFromString} (qui ne connaît pas {@code §x}).</li>
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
            return; // pas de hex : on laisse vanilla/OptiFine dessiner normalement
        }
        FontRenderer self = (FontRenderer) (Object) this;
        cir.setReturnValue(HexTextRenderer.draw(self, text, x, y, color, dropShadow));
    }

    /** Vrai si la chaîne contient une séquence hex {@code §x} (insensible à la casse du {@code x}). */
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

    /** Comme {@code getFormatFromString} vanilla mais en conservant les séquences {@code §x} entières. */
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
                // §x + 6 paires §<hexdigit> : une couleur, qui réinitialise le format accumulé.
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
