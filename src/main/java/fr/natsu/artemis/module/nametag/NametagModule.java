package fr.natsu.artemis.module.nametag;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

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
        List<ByteString> legacyLines = new ArrayList<>();
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    player = ApolloProtos.parseUuid(in.readBytes());
                    break;
                case 2:
                    legacyLines.add(in.readBytes());
                    break;
                case 3:
                    jsonLines.add(in.readString());
                    break;
                default:
                    in.skipField(tag);
            }
        }

        if (player == null) {
            return;
        }

        List<List<AdventureText.Run>> lines = new ArrayList<>();
        if (!jsonLines.isEmpty()) {
            for (String json : jsonLines) {
                lines.add(AdventureText.parse(json));
            }
        } else if (!legacyLines.isEmpty()) {
            for (ByteString component : legacyLines) {
                List<AdventureText.Run> runs = new ArrayList<>();
                parseComponent(component, -1, "", runs);
                lines.add(runs);
            }
        }

        if (lines.isEmpty()) {
            NametagState.reset(player);
            return;
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

    private static void parseComponent(ByteString data, int inheritedRgb, String inheritedCodes,
                                       List<AdventureText.Run> out) throws Exception {
        CodedInputStream in = data.newCodedInput();
        String content = "";
        int rgb = inheritedRgb;
        StringBuilder codes = new StringBuilder(inheritedCodes);
        List<ByteString> children = new ArrayList<>();

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    content = in.readString();
                    break;
                case 2:
                    rgb = ApolloProtos.parseColorArgb(in.readBytes()) & 0xFFFFFF;
                    break;
                case 3:
                    if (WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                        CodedInputStream packed = in.readBytes().newCodedInput();
                        while (!packed.isAtEnd()) {
                            appendDecorator(packed.readInt32(), codes);
                        }
                    } else {
                        appendDecorator(in.readInt32(), codes);
                    }
                    break;
                case 4:
                    children.add(in.readBytes());
                    break;
                default:
                    in.skipField(tag);
            }
        }

        if (!content.isEmpty()) {
            out.add(new AdventureText.Run(codes.toString() + content, rgb < 0 ? 0xFFFFFF : rgb));
        }
        for (ByteString child : children) {
            parseComponent(child, rgb, codes.toString(), out);
        }
    }

    private static void appendDecorator(int value, StringBuilder codes) {
        switch (value) {
            case 1:
                codes.append('§').append('k');
                break;
            case 2:
                codes.append('§').append('l');
                break;
            case 3:
                codes.append('§').append('m');
                break;
            case 4:
                codes.append('§').append('n');
                break;
            case 5:
                codes.append('§').append('o');
                break;
            default:
                break;
        }
    }
}
