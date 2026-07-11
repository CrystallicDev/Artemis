package fr.natsu.artemis.module.team;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import fr.natsu.artemis.module.nametag.AdventureText;

/**
 * Source de vérité du module TeamView : la liste des coéquipiers et leurs positions/couleurs.
 *
 * <p>{@code UpdateTeamMembersMessage} remplace la liste entière ; on publie donc une référence
 * immuable ({@code volatile}) écrite depuis le thread réseau et lue depuis le thread de rendu.</p>
 */
public final class TeamState {

    /** Un coéquipier à marquer dans le monde. */
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
