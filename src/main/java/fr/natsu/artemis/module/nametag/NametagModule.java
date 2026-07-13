package fr.natsu.artemis.module.nametag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Nametag module: turns the {@code lunarclient.apollo.nametag.v1.*} messages into colored nametag
 * lines in {@link NametagState}. The rendering is handled by {@code NametagRenderer}.
 *
 * <p>{@code OverrideNametagMessage}: {@code player_uuid} (#1), {@code lines} (#2, legacy Component),
 * {@code adventure_json_lines} (#3, JSON, the hex path). We prefer field #3.</p>
 */
public final class NametagModule {

    private static final String OVERRIDE = "lunarclient.apollo.nametag.v1.OverrideNametagMessage";
    private static final String RESET = "lunarclient.apollo.nametag.v1.ResetNametagMessage";
    private static final String RESET_ALL = "lunarclient.apollo.nametag.v1.ResetNametagsMessage";

    private NametagModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(OVERRIDE, NametagModule::onOverride);
        network.register(RESET, NametagModule::onReset);
        network.register(RESET_ALL, NametagModule::onResetAll);
    }

    private static void onOverride(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        UUID player = null;
        List<String> jsonLines = new ArrayList<>();
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    player = ApolloProtos.parseUuid(in.readBytes());
                    break;
                case 3:
                    jsonLines.add(in.readString());
                    break;
                default:
                    // #2 (legacy Component lines) and other fields are ignored.
                    in.skipField(tag);
            }
        }

        if (player == null) {
            return;
        }

        if (jsonLines.isEmpty()) {
            // No adventure lines: nothing to show in hex (the legacy #2 isn't handled yet).
            NametagState.reset(player);
            Artemis.LOGGER.warn("[Nametag] override {} without adventure_json_lines (legacy not handled)", player);
            return;
        }

        List<List<AdventureText.Run>> lines = new ArrayList<>(jsonLines.size());
        for (String json : jsonLines) {
            lines.add(AdventureText.parse(json));
        }
        NametagState.override(player, lines);
        Artemis.LOGGER.info("[Nametag] override {} ({} line(s))", player, lines.size());
    }

    private static void onReset(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        UUID player = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                player = ApolloProtos.parseUuid(in.readBytes());
            } else {
                in.skipField(tag);
            }
        }

        if (player != null) {
            NametagState.reset(player);
            Artemis.LOGGER.info("[Nametag] reset {}", player);
        }
    }

    private static void onResetAll(Any message) {
        NametagState.resetAll();
        Artemis.LOGGER.info("[Nametag] reset all");
    }
}
