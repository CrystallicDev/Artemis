package fr.natsu.artemis;

public final class LunarHost {

    private static final String[] MARKER_CLASSES = {
        "com.lunarclient.apollo.ApolloManager",
        "com.moonsworth.lunar.client.Lunar"
    };

    private static Boolean detected;

    private LunarHost() {
    }

    public static boolean isLunar() {
        if (detected == null) {
            detected = detect();
            Artemis.LOGGER.info(detected
                ? "[Artemis] Lunar Client detected: running the Lunar profile (glow, chat, lightning, models, icons, boss bars)"
                : "[Artemis] standalone Forge client: running the full profile");
        }
        return detected;
    }

    private static boolean detect() {
        String forced = System.getProperty("artemis.lunar");
        if (forced != null) {
            return Boolean.parseBoolean(forced);
        }
        ClassLoader[] loaders = {
            LunarHost.class.getClassLoader(),
            Thread.currentThread().getContextClassLoader(),
            ClassLoader.getSystemClassLoader()
        };
        for (String name : MARKER_CLASSES) {
            for (ClassLoader loader : loaders) {
                if (loader == null) {
                    continue;
                }
                try {
                    Class.forName(name, false, loader);
                    return true;
                } catch (Throwable ignored) {
                }
            }
        }
        return false;
    }
}
