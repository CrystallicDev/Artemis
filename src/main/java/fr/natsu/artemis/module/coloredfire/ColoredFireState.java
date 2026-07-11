package fr.natsu.artemis.module.coloredfire;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du module Colored Fire : la couleur RGB des flammes par joueur.
 *
 * <p>Écrit depuis le thread réseau ({@link ColoredFireModule}), lu depuis le thread de rendu
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

    /** Couleur RGB des flammes du joueur, ou {@code 0} s'il n'a pas de couleur custom. */
    public static int colorOrZero(UUID player) {
        Integer rgb = COLORS.get(player);
        return rgb == null ? 0 : rgb;
    }
}
