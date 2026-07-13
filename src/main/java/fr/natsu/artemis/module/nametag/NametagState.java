package fr.natsu.artemis.module.nametag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source of truth for the Nametag module: the custom nametag lines per player, already parsed into
 * colored runs (see {@link AdventureText}).
 *
 * <p>Written from the network thread ({@link NametagModule}), read from the render thread
 * ({@code NametagRenderer}).</p>
 */
public final class NametagState {

    /** Player UUID -> list of lines; each line is a list of runs. */
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

    /** A player's nametag lines, or {@code null} if they have no Apollo override. */
    public static List<List<AdventureText.Run>> get(UUID player) {
        return NAMETAGS.get(player);
    }
}
