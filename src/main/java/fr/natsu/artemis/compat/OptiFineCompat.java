package fr.natsu.artemis.compat;

import java.lang.reflect.Field;

import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;

/**
 * OptiFine compatibility helpers, read through reflection. The fields only exist when OptiFine is
 * installed; without it we just return neutral values.
 */
public final class OptiFineCompat {

    /** OptiFine's {@code GameSettings.ofFastRender} field, or {@code null} when it isn't present. */
    private static final Field FAST_RENDER = resolveField("ofFastRender");

    private OptiFineCompat() {
    }

    private static Field resolveField(String name) {
        try {
            return GameSettings.class.getField(name);
        } catch (NoSuchFieldException e) {
            return null; // OptiFine isn't installed (or the field was renamed): treat it as off.
        }
    }

    /**
     * Whether OptiFine's <b>Fast Render</b> option is enabled. It rewrites the render pipeline in a way
     * that breaks our FBO + outline shader pass (black screen), so we skip Glowing while it's on.
     * Returns {@code false} when OptiFine isn't installed.
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
