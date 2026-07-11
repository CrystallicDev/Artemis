package fr.natsu.artemis.module.waypoint;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source de vérité du module Waypoint : les waypoints actifs, indexés par leur nom.
 *
 * <p>Écrit depuis le thread réseau ({@link WaypointModule}), lu depuis le thread de rendu
 * ({@code WaypointRenderer}).</p>
 */
public final class WaypointState {

    /** Un waypoint à un bloc donné. */
    public static final class Waypoint {
        public final String name;
        public final int x;
        public final int y;
        public final int z;
        public final int colorArgb;
        public final boolean showBeam;
        public final boolean highlightBlock;
        public final float lineWidth;
        public final boolean showText;
        public final boolean showDistance;
        public volatile boolean hidden;

        public Waypoint(String name, int x, int y, int z, int colorArgb, boolean showBeam,
                        boolean highlightBlock, float lineWidth, boolean showText, boolean showDistance,
                        boolean hidden) {
            this.name = name;
            this.x = x;
            this.y = y;
            this.z = z;
            this.colorArgb = colorArgb;
            this.showBeam = showBeam;
            this.highlightBlock = highlightBlock;
            this.lineWidth = lineWidth;
            this.showText = showText;
            this.showDistance = showDistance;
            this.hidden = hidden;
        }
    }

    private static final Map<String, Waypoint> WAYPOINTS = new ConcurrentHashMap<>();

    private WaypointState() {
    }

    public static void display(Waypoint waypoint) {
        WAYPOINTS.put(waypoint.name, waypoint);
    }

    public static void remove(String name) {
        WAYPOINTS.remove(name);
    }

    public static void resetAll() {
        WAYPOINTS.clear();
    }

    /** Change la visibilité d'un waypoint existant. */
    public static void setHidden(String name, boolean hidden) {
        Waypoint waypoint = WAYPOINTS.get(name);
        if (waypoint != null) {
            waypoint.hidden = hidden;
        }
    }

    public static Collection<Waypoint> all() {
        return WAYPOINTS.values();
    }
}
