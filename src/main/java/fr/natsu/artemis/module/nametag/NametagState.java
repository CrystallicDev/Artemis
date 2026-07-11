package fr.natsu.artemis.module.nametag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du module Nametag : les lignes de nametag personnalisées par joueur, déjà
 * parsées en runs colorés (voir {@link AdventureText}).
 *
 * <p>Écrit depuis le thread réseau ({@link NametagModule}), lu depuis le thread de rendu
 * ({@code NametagRenderer}).</p>
 */
public final class NametagState {

    /** UUID joueur -> liste de lignes ; chaque ligne est une liste de runs. */
    private static final Map<UUID, List<List<AdventureText.Run>>> NAMETAGS = new ConcurrentHashMap<>();

    private NametagState() {
    }

    public static void override(UUID player, List<List<AdventureText.Run>> lines) {
        NAMETAGS.put(player, lines);
    }

    public static void reset(UUID player) {
        NAMETAGS.remove(player);
    }

    public static void resetAll() {
        NAMETAGS.clear();
    }

    /** Lignes de nametag d'un joueur, ou {@code null} s'il n'a pas d'override Apollo. */
    public static List<List<AdventureText.Run>> get(UUID player) {
        return NAMETAGS.get(player);
    }
}
