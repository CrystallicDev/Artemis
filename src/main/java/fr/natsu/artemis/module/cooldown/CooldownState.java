package fr.natsu.artemis.module.cooldown;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

/**
 * Source de vérité du module Cooldown : les cooldowns actifs, indexés par leur nom.
 *
 * <p>Écrit depuis le thread réseau ({@link CooldownModule}), lu et purgé depuis le thread de rendu
 * ({@code CooldownRenderer}).</p>
 */
public final class CooldownState {

    /** Un cooldown en cours d'affichage. */
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

        /** Fraction restante dans [0,1]. */
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

    /** Renvoie les cooldowns encore actifs, en purgeant au passage ceux expirés. */
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
