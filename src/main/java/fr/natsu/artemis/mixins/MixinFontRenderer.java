package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.module.chat.HexTextRenderer;
import fr.natsu.artemis.module.chat.LunarFontBridge;
import fr.natsu.artemis.module.icon.IconTextRenderer;
import net.minecraft.client.gui.FontRenderer;

@Mixin(FontRenderer.class)
public class MixinFontRenderer {

    private static final char SECTION = '§';

    @Inject(
        method = "renderString(Ljava/lang/String;FFIZ)I",
        at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$hexRender(String text, float x, float y, int color, boolean shadowPass,
            CallbackInfoReturnable<Integer> cir) {
        if (text == null) {
            return;
        }
        FontRenderer self = (FontRenderer) (Object) this;
        boolean icons = IconTextRenderer.contains(text);
        if (!icons && !containsHex(text)) {
            return;
        }
        Object holder = LunarFontBridge.current(self);
        int drawColor = color;
        if (holder != null) {
            int alpha = LunarFontBridge.alphaOf(holder, x, y, (color >>> 24) & 0xFF);
            if (alpha < 4) {
                return;
            }
            drawColor = (alpha << 24) | (color & 0xFFFFFF);
            LunarFontBridge.suspend(self);
        }
        try {
            cir.setReturnValue(icons
                ? IconTextRenderer.draw(self, text, x, y, drawColor, false, shadowPass)
                : HexTextRenderer.draw(self, text, x, y, drawColor, false, shadowPass));
        } finally {
            LunarFontBridge.restore(self, holder);
        }
    }

    @Inject(method = "getStringWidth", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$iconWidth(String text, CallbackInfoReturnable<Integer> cir) {
        if (text == null || !IconTextRenderer.contains(text)) {
            return;
        }
        cir.setReturnValue(IconTextRenderer.width((FontRenderer) (Object) this, text));
    }

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
