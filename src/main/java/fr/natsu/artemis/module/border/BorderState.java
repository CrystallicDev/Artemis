package fr.natsu.artemis.module.border;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class BorderState {

    public static final class Border {

        private final double fromMinX;
        private final double fromMinZ;
        private final double fromMaxX;
        private final double fromMaxZ;
        private final double toMinX;
        private final double toMinZ;
        private final double toMaxX;
        private final double toMaxZ;
        private final long startMillis;
        private final long durationMillis;
        public final int argb;

        Border(double fromMinX, double fromMinZ, double fromMaxX, double fromMaxZ,
               double toMinX, double toMinZ, double toMaxX, double toMaxZ,
               long startMillis, long durationMillis, int argb) {
            this.fromMinX = fromMinX;
            this.fromMinZ = fromMinZ;
            this.fromMaxX = fromMaxX;
            this.fromMaxZ = fromMaxZ;
            this.toMinX = toMinX;
            this.toMinZ = toMinZ;
            this.toMaxX = toMaxX;
            this.toMaxZ = toMaxZ;
            this.startMillis = startMillis;
            this.durationMillis = durationMillis;
            this.argb = argb;
        }

        public double[] bounds() {
            if (this.durationMillis <= 0L) {
                return new double[] {this.toMinX, this.toMinZ, this.toMaxX, this.toMaxZ};
            }
            double t = (System.currentTimeMillis() - this.startMillis) / (double) this.durationMillis;
            if (t >= 1.0D) {
                return new double[] {this.toMinX, this.toMinZ, this.toMaxX, this.toMaxZ};
            }
            if (t < 0.0D) {
                t = 0.0D;
            }
            return new double[] {
                lerp(this.fromMinX, this.toMinX, t),
                lerp(this.fromMinZ, this.toMinZ, t),
                lerp(this.fromMaxX, this.toMaxX, t),
                lerp(this.fromMaxZ, this.toMaxZ, t)
            };
        }

        private static double lerp(double a, double b, double t) {
            return a + (b - a) * t;
        }
    }

    private static final Map<String, Border> BORDERS = new ConcurrentHashMap<>();

    private BorderState() {
    }

    public static void display(String id, double minX, double minZ, double maxX, double maxZ,
                               int argb, boolean animate, int durationTicks) {
        Border existing = BORDERS.get(id);
        if (animate && durationTicks > 0 && existing != null) {
            double[] from = existing.bounds();
            BORDERS.put(id, new Border(from[0], from[1], from[2], from[3],
                minX, minZ, maxX, maxZ, System.currentTimeMillis(), durationTicks * 50L, argb));
        } else {
            BORDERS.put(id, new Border(minX, minZ, maxX, maxZ, minX, minZ, maxX, maxZ, 0L, 0L, argb));
        }
    }

    public static void remove(String id) {
        BORDERS.remove(id);
    }

    public static void resetAll() {
        BORDERS.clear();
    }

    public static Collection<Border> all() {
        return BORDERS.values();
    }

    public static boolean isEmpty() {
        return BORDERS.isEmpty();
    }
}
