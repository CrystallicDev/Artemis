package fr.natsu.artemis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Configuration persistée d'Artemis (fichier {@code config/artemis.properties}).
 *
 * <p>Positions HUD stockées en fractions de l'écran « scaled » (0..1) pour rester correctes quelle
 * que soit la résolution / le facteur GUI.</p>
 */
public final class ArtemisConfig {

    /** Sens de disposition des cooldowns depuis le point d'ancrage. */
    public enum CooldownDirection {
        RIGHT, LEFT, DOWN, UP;

        public CooldownDirection next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /** Clé de traduction du nom de la direction. */
        public String translationKey() {
            return "artemis.direction." + name().toLowerCase();
        }
    }

    /** Position (ancre) du HUD des cooldowns, en fraction de l'écran. */
    public static float cooldownX = 0.5F;
    public static float cooldownY = 0.72F;
    public static CooldownDirection cooldownDirection = CooldownDirection.RIGHT;

    public static final float DEFAULT_COOLDOWN_X = 0.5F;
    public static final float DEFAULT_COOLDOWN_Y = 0.72F;

    private static File file;

    private ArtemisConfig() {
    }

    public static void init(File configDir) {
        file = new File(configDir, "artemis.properties");
        load();
    }

    public static void load() {
        if (file == null || !file.exists()) {
            return;
        }
        Properties properties = new Properties();
        try (FileInputStream in = new FileInputStream(file)) {
            properties.load(in);
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Config] lecture impossible : {}", e.toString());
            return;
        }
        cooldownX = readFloat(properties, "cooldownX", cooldownX);
        cooldownY = readFloat(properties, "cooldownY", cooldownY);
        cooldownDirection = readDirection(properties, "cooldownDirection", cooldownDirection);
    }

    public static void save() {
        if (file == null) {
            return;
        }
        Properties properties = new Properties();
        properties.setProperty("cooldownX", Float.toString(cooldownX));
        properties.setProperty("cooldownY", Float.toString(cooldownY));
        properties.setProperty("cooldownDirection", cooldownDirection.name());
        try (FileOutputStream out = new FileOutputStream(file)) {
            properties.store(out, "Artemis configuration");
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Config] ecriture impossible : {}", e.toString());
        }
    }

    private static float readFloat(Properties properties, String key, float fallback) {
        try {
            return Float.parseFloat(properties.getProperty(key));
        } catch (Exception e) {
            return fallback;
        }
    }

    private static CooldownDirection readDirection(Properties properties, String key, CooldownDirection fallback) {
        try {
            return CooldownDirection.valueOf(properties.getProperty(key));
        } catch (Exception e) {
            return fallback;
        }
    }
}
