package fr.natsu.artemis.module.beam;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BeamState {

    public static final class Beam {

        public final int x;
        public final int y;
        public final int z;
        public final int argb;

        public Beam(int x, int y, int z, int argb) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.argb = argb;
        }
    }

    private static final Map<String, Beam> BEAMS = new ConcurrentHashMap<>();

    private BeamState() {
    }

    public static void display(String id, Beam beam) {
        BEAMS.put(id, beam);
    }

    public static void remove(String id) {
        BEAMS.remove(id);
    }

    public static void resetAll() {
        BEAMS.clear();
    }

    public static Collection<Beam> all() {
        return BEAMS.values();
    }

    public static boolean isEmpty() {
        return BEAMS.isEmpty();
    }
}
