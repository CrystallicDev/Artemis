package fr.natsu.artemis.module.waypoint;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Source of truth for the Waypoint module: the active waypoints, keyed by their name.
 *
 * <p>Written from the network thread ({@link WaypointModule}), read from the render thread
 * ({@code WaypointRenderer}).</p>
 */
public final class WaypointState {

    /** A waypoint at a given block. */
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

    /** Toggles the visibility of an existing waypoint. */
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
