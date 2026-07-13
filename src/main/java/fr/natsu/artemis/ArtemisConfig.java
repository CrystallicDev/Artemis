package fr.natsu.artemis;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.Properties;

/**
 * Persisted Artemis configuration (the {@code config/artemis.properties} file).
 *
 * <p>HUD positions are stored as fractions of the "scaled" screen (0..1) so they stay correct
 * whatever the resolution or GUI scale is.</p>
 */
public final class ArtemisConfig {

    /** Layout direction of the cooldowns from the anchor point. */
    public enum CooldownDirection {
        RIGHT, LEFT, DOWN, UP;

        public CooldownDirection next() {
            return values()[(ordinal() + 1) % values().length];
        }

        /** Translation key for the direction's name. */
        public String translationKey() {
            return "artemis.direction." + name().toLowerCase();
        }
    }

    /** Cooldown HUD anchor position, as a fraction of the screen. */
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
            Artemis.LOGGER.warn("[Config] could not read: {}", e.toString());
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
            Artemis.LOGGER.warn("[Config] could not write: {}", e.toString());
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
