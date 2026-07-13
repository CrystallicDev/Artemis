package fr.natsu.artemis.module.lightning;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * State of the active colored lightning bolts (a non-Apollo, Artemis-only feature).
 *
 * <p>Every bolt is purely visual: a world position, an outer (glow) color and a core color, a seed
 * that freezes its shape, and a spawn time. They live for {@link #LIFETIME_MS} and are then dropped
 * at render time. Fed by the network thread ({@link ArtemisLightning}) and read by the render thread,
 * hence the concurrent list.</p>
 */
public final class LightningState {

    /** How long a bolt lives, in milliseconds. */
    public static final long LIFETIME_MS = 900L;

    private static final Random SEED_RANDOM = new Random();
    private static final List<Bolt> BOLTS = new CopyOnWriteArrayList<>();

    private LightningState() {
    }

    /** Adds a bolt at the given position/colors (colors are ARGB, alpha ignored). */
    public static void add(double x, double y, double z, int mainArgb, int coreArgb) {
        BOLTS.add(new Bolt(x, y, z, mainArgb, coreArgb, SEED_RANDOM.nextLong(), System.currentTimeMillis()));
    }

    /** The bolts still alive; drops the expired ones along the way. */
    public static List<Bolt> active() {
        long now = System.currentTimeMillis();
        BOLTS.removeIf(bolt -> now - bolt.spawnTime >= LIFETIME_MS);
        return BOLTS;
    }

    public static boolean isEmpty() {
        return BOLTS.isEmpty();
    }

    /** A colored bolt: position, pre-split outer/core colors, seed and spawn time. */
    public static final class Bolt {

        public final double x;
        public final double y;
        public final double z;
        public final float mainRed;
        public final float mainGreen;
        public final float mainBlue;
        public final float coreRed;
        public final float coreGreen;
        public final float coreBlue;
        public final long seed;
        public final long spawnTime;

        Bolt(double x, double y, double z, int mainArgb, int coreArgb, long seed, long spawnTime) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.mainRed = ((mainArgb >> 16) & 0xFF) / 255.0F;
            this.mainGreen = ((mainArgb >> 8) & 0xFF) / 255.0F;
            this.mainBlue = (mainArgb & 0xFF) / 255.0F;
            this.coreRed = ((coreArgb >> 16) & 0xFF) / 255.0F;
            this.coreGreen = ((coreArgb >> 8) & 0xFF) / 255.0F;
            this.coreBlue = (coreArgb & 0xFF) / 255.0F;
            this.seed = seed;
            this.spawnTime = spawnTime;
        }

        /** Master alpha based on age: full during the flash, fading out near the end of life. */
        public float alpha(long now) {
            float age = (now - this.spawnTime) / (float) LIFETIME_MS;
            if (age < 0.0F) {
                age = 0.0F;
            }
            // Full flash for the first 40%, then a linear fade down to 0.
            return age < 0.4F ? 1.0F : Math.max(0.0F, 1.0F - (age - 0.4F) / 0.6F);
        }
    }
}
