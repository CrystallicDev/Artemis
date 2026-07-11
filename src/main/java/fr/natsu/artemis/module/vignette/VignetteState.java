package fr.natsu.artemis.module.vignette;

import net.minecraft.util.ResourceLocation;

/**
 * Source de vérité du module Vignette : la texture d'overlay plein écran courante et son opacité.
 *
 * <p>Écrit depuis le thread réseau ({@link VignetteModule}), lu depuis le thread de rendu
 * ({@code VignetteRenderer}). {@code volatile} suffit (publication d'une référence immuable).</p>
 */
public final class VignetteState {

    private static volatile ResourceLocation texture;
    private static volatile float opacity;

    private VignetteState() {
    }

    public static void display(ResourceLocation texture, float opacity) {
        VignetteState.texture = texture;
        VignetteState.opacity = opacity;
    }

    public static void reset() {
        VignetteState.texture = null;
    }

    /** Texture de vignette active, ou {@code null} si aucune. */
    public static ResourceLocation texture() {
        return texture;
    }

    public static float opacity() {
        return opacity;
    }
}
