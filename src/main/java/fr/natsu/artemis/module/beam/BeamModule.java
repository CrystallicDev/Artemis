package fr.natsu.artemis.module.beam;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

public final class BeamModule {

    private static final String DISPLAY = "lunarclient.apollo.beam.v1.DisplayBeaconBeamMessage";
    private static final String REMOVE = "lunarclient.apollo.beam.v1.RemoveBeaconBeamMessage";
    private static final String RESET = "lunarclient.apollo.beam.v1.ResetBeaconBeamsMessage";

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private BeamModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, BeamModule::onDisplay);
        network.register(REMOVE, BeamModule::onRemove);
        network.register(RESET, BeamModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        int color = DEFAULT_COLOR;
        int[] loc = null;

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    id = in.readString();
                    break;
                case 2:
                    color = ApolloProtos.parseColorArgb(in.readBytes());
                    break;
                case 3:
                    loc = parseBlockLocation(in.readBytes());
                    break;
                default:
                    in.skipField(tag);
            }
        }

        if (id == null || loc == null) {
            return;
        }
        BeamState.display(id, new BeamState.Beam(loc[0], loc[1], loc[2], color));
        Artemis.LOGGER.info("[Beam] display '{}' ({}, {}, {}) color={}", id, loc[0], loc[1], loc[2],
            String.format("#%06X", color & 0xFFFFFF));
    }

    private static void onRemove(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                id = in.readString();
            } else {
                in.skipField(tag);
            }
        }
        if (id != null) {
            BeamState.remove(id);
            Artemis.LOGGER.info("[Beam] remove '{}'", id);
        }
    }

    private static void onReset(Any message) {
        BeamState.resetAll();
        Artemis.LOGGER.info("[Beam] reset");
    }

    private static int[] parseBlockLocation(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        int x = 0;
        int y = 0;
        int z = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 2:
                    x = in.readSInt32();
                    break;
                case 3:
                    y = in.readSInt32();
                    break;
                case 4:
                    z = in.readSInt32();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new int[] {x, y, z};
    }
}
