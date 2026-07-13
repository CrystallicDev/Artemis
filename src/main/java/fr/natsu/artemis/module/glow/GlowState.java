package fr.natsu.artemis.module.glow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source of truth for the Glow module: the players currently glowing and their color.
 *
 * <p>Written from the network thread (via {@link GlowModule}) and read from the render thread (via
 * {@code GlowRenderer}). {@link ConcurrentHashMap} keeps it consistent without a lock.</p>
 */
public final class GlowState {

    /** Default RGB color (white) when Apollo asks for a glow without an explicit color. */
    public static final int DEFAULT_RGB = 0xFFFFFF;

    private static final Map<UUID, Integer> GLOWING = new ConcurrentHashMap<>();

    private GlowState() {
    }

    /**
     * Turns on (or updates) a player's glow.
     *
     * @param player the player to make glow
     * @param argb   the requested ARGB color, or {@code null} for the default color
     */
    public static void override(UUID player, Integer argb) {
        GLOWING.put(player, argb == null ? DEFAULT_RGB : (argb & 0xFFFFFF));
    }

    /** Turns off a player's glow. */
    public static void reset(UUID player) {
        GLOWING.remove(player);
    }

    /** Turns off every glow. */
    public static void resetAll() {
        GLOWING.clear();
    }

    public static boolean isGlowing(UUID player) {
        return GLOWING.containsKey(player);
    }

    /** A player's RGB color (no alpha), or {@link #DEFAULT_RGB} if they aren't glowing. */
    public static int color(UUID player) {
        Integer rgb = GLOWING.get(player);
        return rgb == null ? DEFAULT_RGB : rgb;
    }

    public static boolean isEmpty() {
        return GLOWING.isEmpty();
    }
}
