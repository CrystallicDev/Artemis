package fr.natsu.artemis.module.team;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.module.nametag.AdventureText;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Module TeamView : traduit {@code lunarclient.apollo.team.v1.*} en liste de {@link TeamState}.
 * Rendu world-space assuré par {@code TeamRenderer}.
 *
 * <p>{@code UpdateTeamMembersMessage} : members(#1 repeated TeamMember). Chaque {@code TeamMember} :
 * player_uuid(#1 Uuid), player_display_name(#2 Component, ignoré), location(#3 Location),
 * marker_color(#4 Color), adventure_json_player_name(#5 string, voie hex du nom).</p>
 */
public final class TeamModule {

    private static final String UPDATE = "lunarclient.apollo.team.v1.UpdateTeamMembersMessage";
    private static final String RESET = "lunarclient.apollo.team.v1.ResetTeamMembersMessage";

    private static final int DEFAULT_MARKER = 0xFFFFFFFF;

    private TeamModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(UPDATE, TeamModule::onUpdate);
        network.register(RESET, TeamModule::onReset);
    }

    private static void onUpdate(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        List<TeamState.Member> members = new ArrayList<>();
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                TeamState.Member member = parseMember(in.readBytes());
                if (member != null) {
                    members.add(member);
                }
            } else {
                in.skipField(tag);
            }
        }

        TeamState.update(members);
        Artemis.LOGGER.info("[TeamView] {} coequipier(s)", members.size());
    }

    private static void onReset(Any message) {
        TeamState.reset();
        Artemis.LOGGER.info("[TeamView] reset");
    }

    private static TeamState.Member parseMember(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        UUID uuid = null;
        double[] loc = null;
        int markerArgb = DEFAULT_MARKER;
        List<AdventureText.Run> name = Collections.emptyList();

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    uuid = ApolloProtos.parseUuid(in.readBytes());
                    break;
                case 3:
                    loc = parseLocation(in.readBytes());
                    break;
                case 4:
                    markerArgb = ApolloProtos.parseColorArgb(in.readBytes());
                    break;
                case 5:
                    name = AdventureText.parse(in.readString());
                    break;
                default:
                    // #2 (Component legacy) et autres champs ignorés.
                    in.skipField(tag);
            }
        }

        if (uuid == null || loc == null) {
            return null;
        }
        return new TeamState.Member(uuid, loc[0], loc[1], loc[2], markerArgb, name);
    }

    /** Parse un {@code Location} : world(#1 string, ignoré), x(#2), y(#3), z(#4) doubles. */
    private static double[] parseLocation(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        double x = 0;
        double y = 0;
        double z = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 2:
                    x = in.readDouble();
                    break;
                case 3:
                    y = in.readDouble();
                    break;
                case 4:
                    z = in.readDouble();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new double[] {x, y, z};
    }
}
