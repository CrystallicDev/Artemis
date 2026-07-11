package fr.natsu.artemis.module.glow;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du module Glow : les joueurs actuellement en surbrillance et leur couleur.
 *
 * <p>Écrit depuis le thread réseau (via {@link GlowModule}) et lu depuis le thread de rendu (via
 * {@code GlowRenderer}). {@link ConcurrentHashMap} garantit la cohérence sans verrou.</p>
 */
public final class GlowState {

    /** Couleur RGB par défaut (blanc) quand Apollo demande un glow sans couleur explicite. */
    public static final int DEFAULT_RGB = 0xFFFFFF;

    private static final Map<UUID, Integer> GLOWING = new ConcurrentHashMap<>();

    private GlowState() {
    }

    /**
     * Active (ou met à jour) la surbrillance d'un joueur.
     *
     * @param player le joueur à faire briller
     * @param argb   la couleur ARGB demandée, ou {@code null} pour la couleur par défaut
     */
    public static void override(UUID player, Integer argb) {
        GLOWING.put(player, argb == null ? DEFAULT_RGB : (argb & 0xFFFFFF));
    }

    /** Désactive la surbrillance d'un joueur. */
    public static void reset(UUID player) {
        GLOWING.remove(player);
    }

    /** Désactive toute surbrillance. */
    public static void resetAll() {
        GLOWING.clear();
    }

    public static boolean isGlowing(UUID player) {
        return GLOWING.containsKey(player);
    }

    /** Couleur RGB (sans alpha) d'un joueur, ou {@link #DEFAULT_RGB} s'il ne brille pas. */
    public static int color(UUID player) {
        Integer rgb = GLOWING.get(player);
        return rgb == null ? DEFAULT_RGB : rgb;
    }

    public static boolean isEmpty() {
        return GLOWING.isEmpty();
    }
}
