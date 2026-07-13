package fr.natsu.artemis.module.cooldown;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

/**
 * Source of truth for the Cooldown module: the active cooldowns, keyed by their name.
 *
 * <p>Written from the network thread ({@link CooldownModule}), read and purged from the render thread
 * ({@code CooldownRenderer}).</p>
 */
public final class CooldownState {

    /** A cooldown currently being shown. */
    public static final class Entry {
        public final String name;
        public final long startMillis;
        public final long durationMillis;
        public final ItemStack icon;
        public final int circleStartArgb;
        public final int circleEndArgb;
        public final int circleEdgeArgb;
        public final int textArgb;

        Entry(String name, long durationMillis, ItemStack icon,
              int circleStartArgb, int circleEndArgb, int circleEdgeArgb, int textArgb) {
            this.name = name;
            this.startMillis = System.currentTimeMillis();
            this.durationMillis = durationMillis;
            this.icon = icon;
            this.circleStartArgb = circleStartArgb;
            this.circleEndArgb = circleEndArgb;
            this.circleEdgeArgb = circleEdgeArgb;
            this.textArgb = textArgb;
        }

        public long remainingMillis() {
            long elapsed = System.currentTimeMillis() - this.startMillis;
            return Math.max(0L, this.durationMillis - elapsed);
        }

        /** Remaining fraction in [0,1]. */
        public float remainingFraction() {
            if (this.durationMillis <= 0L) {
                return 0F;
            }
            return Math.max(0F, Math.min(1F, remainingMillis() / (float) this.durationMillis));
        }

        public boolean isExpired() {
            return remainingMillis() <= 0L;
        }
    }

    private static final Map<String, Entry> COOLDOWNS = new ConcurrentHashMap<>();

    private CooldownState() {
    }

    public static void display(String name, long durationMillis, ItemStack icon,
                               int circleStartArgb, int circleEndArgb, int circleEdgeArgb, int textArgb) {
        COOLDOWNS.put(name, new Entry(name, durationMillis, icon,
            circleStartArgb, circleEndArgb, circleEdgeArgb, textArgb));
    }

    public static void remove(String name) {
        COOLDOWNS.remove(name);
    }

    public static void resetAll() {
        COOLDOWNS.clear();
    }

    /** Returns the still-active cooldowns, purging the expired ones along the way. */
    public static List<Entry> active() {
        List<Entry> result = new ArrayList<>();
        Iterator<Map.Entry<String, Entry>> it = COOLDOWNS.entrySet().iterator();
        while (it.hasNext()) {
            Entry entry = it.next().getValue();
            if (entry.isExpired()) {
                it.remove();
            } else {
                result.add(entry);
            }
        }
        return result;
    }
}
