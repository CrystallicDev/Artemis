package fr.natsu.artemis.compat;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

/**
 * Ponts de compatibilité avec OptiFine, lus par réflexion (les champs n'existent qu'avec OptiFine
 * installé ; en leur absence on renvoie des valeurs neutres).
 */
public final class OptiFineCompat {

    /** Champ {@code GameSettings.ofFastRender} ajouté par OptiFine, ou {@code null} si absent. */
    private static final Field FAST_RENDER = resolveField("ofFastRender");

    private OptiFineCompat() {
    }

    private static Field resolveField(String name) {
        try {
            return GameSettings.class.getField(name);
        } catch (NoSuchFieldException e) {
            return null; // OptiFine absent (ou champ renommé) : traité comme désactivé.
        }
    }

    /**
     * Indique si l'option OptiFine <b>Fast Render</b> est active. Elle réécrit le pipeline de rendu
     * d'une façon incompatible avec notre passe FBO + shader d'outline (écran noir) : on doit donc
     * désactiver le Glowing tant qu'elle est active. Renvoie {@code false} sans OptiFine.
     */
    public static boolean isFastRender() {
        if (FAST_RENDER == null) {
            return false;
        }
        try {
            return FAST_RENDER.getBoolean(Minecraft.getMinecraft().gameSettings);
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
