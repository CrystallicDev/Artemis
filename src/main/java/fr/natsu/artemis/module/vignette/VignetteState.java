package fr.natsu.artemis.module.vignette;

import net.minecraft.util.ResourceLocation;

/**
 * Source of truth for the Vignette module: the current full-screen overlay texture and its opacity.
 *
 * <p>Written from the network thread ({@link VignetteModule}), read from the render thread
 * ({@code VignetteRenderer}). {@code volatile} is enough (publishing an immutable reference).</p>
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

    /** The active vignette texture, or {@code null} if none. */
    public static ResourceLocation texture() {
        return texture;
    }

    public static float opacity() {
        return opacity;
    }
}
