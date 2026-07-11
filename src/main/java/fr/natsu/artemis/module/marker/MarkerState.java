package fr.natsu.artemis.module.marker;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import net.minecraft.item.ItemStack;

/**
 * Source de vérité du module Marker : les markers actifs, indexés par leur id.
 *
 * <p>Écrit depuis le thread réseau ({@link MarkerModule}), lu depuis le thread de rendu
 * ({@code MarkerRenderer}).</p>
 */
public final class MarkerState {

    /** Types de marker ({@code MarkerFlag}). */
    public static final int TYPE_NORMAL = 1;
    public static final int TYPE_DANGER = 2;
    public static final int TYPE_INFO = 3;
    public static final int TYPE_INTEREST = 4;

    /** Conditions d'affichage ({@code MarkerDisplayCondition}). */
    public static final int COND_UNSPECIFIED = 0;
    public static final int COND_NEVER = 1;
    public static final int COND_HOVER = 2;
    public static final int COND_ALWAYS = 3;

    /** {@code MarkerOwnerDisplay}. */
    public static final int OWNER_HEAD = 1;
    public static final int OWNER_NAME = 2;

    /** Types de cible ({@code MarkerTarget}). */
    public static final int TARGET_NONE = 0;
    public static final int TARGET_ITEM = 1;
    public static final int TARGET_BLOCK = 2;
    public static final int TARGET_ENTITY = 3;
    public static final int TARGET_PLAYER = 4;

    /** Ce que le marker désigne (détermine l'icône rendue). */
    public static final class Target {
        public final int type;
        public final ItemStack item;
        public final String entityType;
        public final UUID playerId;
        public final String playerName;

        public Target(int type, ItemStack item, String entityType, UUID playerId, String playerName) {
            this.type = type;
            this.item = item;
            this.entityType = entityType;
            this.playerId = playerId;
            this.playerName = playerName;
        }

        public static Target none() {
            return new Target(TARGET_NONE, null, null, null, null);
        }
    }

    /** Un marker à une position donnée. */
    public static final class Marker {
        public final String id;
        public final String ownerName;
        public final double x;
        public final double y;
        public final double z;
        public final int type;
        public final int iconArgb;
        public final Target target;
        public final int showOwner;
        public final int showCoordinates;
        public final int showDistance;
        public final int showDescription;
        public final int ownerDisplay;
        public final boolean textShadow;
        public final float scale;

        public Marker(String id, String ownerName, double x, double y, double z, int type, int iconArgb,
                      Target target, int showOwner, int showCoordinates, int showDistance, int showDescription,
                      int ownerDisplay, boolean textShadow, float scale) {
            this.id = id;
            this.ownerName = ownerName;
            this.x = x;
            this.y = y;
            this.z = z;
            this.type = type;
            this.iconArgb = iconArgb;
            this.target = target;
            this.showOwner = showOwner;
            this.showCoordinates = showCoordinates;
            this.showDistance = showDistance;
            this.showDescription = showDescription;
            this.ownerDisplay = ownerDisplay;
            this.textShadow = textShadow;
            this.scale = scale;
        }
    }

    private static final Map<String, Marker> MARKERS = new ConcurrentHashMap<>();

    private MarkerState() {
    }

    public static void display(Marker marker) {
        MARKERS.put(marker.id, marker);
    }

    public static void remove(String id) {
        MARKERS.remove(id);
    }

    public static void resetAll() {
        MARKERS.clear();
    }

    public static Collection<Marker> all() {
        return MARKERS.values();
    }
}
