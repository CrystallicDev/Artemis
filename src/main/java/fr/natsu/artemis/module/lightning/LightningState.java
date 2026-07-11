package fr.natsu.artemis.module.lightning;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * État des éclairs colorés actifs (feature hors Apollo, propre à Artemis).
 *
 * <p>Chaque éclair est purement visuel : une position monde, une couleur externe (glow) et une
 * couleur de cœur, une graine figeant sa forme, et un instant d'apparition. Ils vivent
 * {@link #LIFETIME_MS} puis sont retirés au rendu. Alimenté par le thread réseau
 * ({@link ArtemisLightning}) et lu par le thread de rendu (d'où la liste concurrente).</p>
 */
public final class LightningState {

    /** Durée de vie d'un éclair, en millisecondes. */
    public static final long LIFETIME_MS = 900L;

    private static final Random SEED_RANDOM = new Random();
    private static final List<Bolt> BOLTS = new CopyOnWriteArrayList<>();

    private LightningState() {
    }

    /** Ajoute un éclair aux positions/couleurs données (couleurs en ARGB, alpha ignoré). */
    public static void add(double x, double y, double z, int mainArgb, int coreArgb) {
        BOLTS.add(new Bolt(x, y, z, mainArgb, coreArgb, SEED_RANDOM.nextLong(), System.currentTimeMillis()));
    }

    /** Les éclairs encore vivants ; purge au passage ceux qui ont expiré. */
    public static List<Bolt> active() {
        long now = System.currentTimeMillis();
        BOLTS.removeIf(bolt -> now - bolt.spawnTime >= LIFETIME_MS);
        return BOLTS;
    }

    public static boolean isEmpty() {
        return BOLTS.isEmpty();
    }

    /** Un éclair coloré : position, couleurs (externe + cœur) pré-décomposées, graine, apparition. */
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

        /** Alpha maître selon l'âge : plein au flash, fondu sur la fin de vie. */
        public float alpha(long now) {
            float age = (now - this.spawnTime) / (float) LIFETIME_MS;
            if (age < 0.0F) {
                age = 0.0F;
            }
            // Flash plein sur les 40 % premiers, puis fondu linéaire jusqu'à 0.
            return age < 0.4F ? 1.0F : Math.max(0.0F, 1.0F - (age - 0.4F) / 0.6F);
        }
    }
}
