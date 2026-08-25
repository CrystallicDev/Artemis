package fr.natsu.artemis.module.hologram;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import fr.natsu.artemis.module.nametag.AdventureText.Run;

public final class HologramState {

    public static final class Hologram {

        public final double x;
        public final double y;
        public final double z;
        public final List<List<Run>> lines;
        public final boolean showThroughWalls;
        public final boolean showShadow;
        public final boolean showBackground;

        public Hologram(double x, double y, double z, List<List<Run>> lines,
                        boolean showThroughWalls, boolean showShadow, boolean showBackground) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.lines = lines;
            this.showThroughWalls = showThroughWalls;
            this.showShadow = showShadow;
            this.showBackground = showBackground;
        }
    }

    private static final Map<String, Hologram> HOLOGRAMS = new ConcurrentHashMap<>();

    private HologramState() {
    }

    public static void display(String id, Hologram hologram) {
        HOLOGRAMS.put(id, hologram);
    }

    public static void remove(String id) {
        HOLOGRAMS.remove(id);
    }

    public static void resetAll() {
        HOLOGRAMS.clear();
    }

    public static Collection<Hologram> all() {
        return HOLOGRAMS.values();
    }

    public static boolean isEmpty() {
        return HOLOGRAMS.isEmpty();
    }
}
