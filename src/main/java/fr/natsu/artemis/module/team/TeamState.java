package fr.natsu.artemis.module.team;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fr.natsu.artemis.module.nametag.AdventureText;

/**
 * Source of truth for the TeamView module: the list of teammates and their positions/colors.
 *
 * <p>{@code UpdateTeamMembersMessage} replaces the whole list, so we publish an immutable reference
 * ({@code volatile}) written from the network thread and read from the render thread.</p>
 */
public final class TeamState {

    /** A teammate to mark in the world. */
    public static final class Member {
        public final UUID uuid;
        public final double x;
        public final double y;
        public final double z;
        public final int markerArgb;
        public final List<AdventureText.Run> name;

        public Member(UUID uuid, double x, double y, double z, int markerArgb, List<AdventureText.Run> name) {
            this.uuid = uuid;
            this.x = x;
            this.y = y;
            this.z = z;
            this.markerArgb = markerArgb;
            this.name = name;
        }
    }

    private static volatile List<Member> members = Collections.emptyList();

    private TeamState() {
    }

    public static void update(List<Member> members) {
        TeamState.members = Collections.unmodifiableList(members);
    }

    public static void reset() {
        TeamState.members = Collections.emptyList();
    }

    public static List<Member> members() {
        return members;
    }
}
