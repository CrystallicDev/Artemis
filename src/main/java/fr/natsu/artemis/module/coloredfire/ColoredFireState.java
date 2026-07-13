package fr.natsu.artemis.module.coloredfire;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source of truth for the Colored Fire module: the RGB flame color per player.
 *
 * <p>Written from the network thread ({@link ColoredFireModule}), read from the render thread
 * ({@code MixinRenderFire}).</p>
 */
public final class ColoredFireState {

    private static final Map<UUID, Integer> COLORS = new ConcurrentHashMap<>();

    private ColoredFireState() {
    }

    public static void override(UUID player, int rgb) {
        COLORS.put(player, rgb & 0xFFFFFF);
    }

    public static void reset(UUID player) {
        COLORS.remove(player);
    }

    public static void resetAll() {
        COLORS.clear();
    }

    public static boolean isEmpty() {
        return COLORS.isEmpty();
    }

    /** The player's RGB flame color, or {@code 0} if they have no custom color. */
    public static int colorOrZero(UUID player) {
        Integer rgb = COLORS.get(player);
        return rgb == null ? 0 : rgb;
    }
}
