package fr.natsu.artemis.module.border;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

public final class BorderModule {

    private static final String DISPLAY = "lunarclient.apollo.border.v1.DisplayBorderMessage";
    private static final String REMOVE = "lunarclient.apollo.border.v1.RemoveBorderMessage";
    private static final String RESET = "lunarclient.apollo.border.v1.ResetBordersMessage";

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private BorderModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, BorderModule::onDisplay);
        network.register(REMOVE, BorderModule::onRemove);
        network.register(RESET, BorderModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        int color = DEFAULT_COLOR;
        double[] bounds = null;
        boolean canShrinkOrExpand = false;
        int durationTicks = 0;

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    id = in.readString();
                    break;
                case 5:
                    canShrinkOrExpand = in.readBool();
                    break;
                case 6:
                    color = ApolloProtos.parseColorArgb(in.readBytes());
                    break;
                case 7:
                    bounds = parseCuboid2D(in.readBytes());
                    break;
                case 8:
                    durationTicks = in.readInt32();
                    break;
                default:
                    in.skipField(tag);
            }
        }

        if (id == null || bounds == null) {
            return;
        }
        BorderState.display(id, bounds[0], bounds[1], bounds[2], bounds[3], color, canShrinkOrExpand, durationTicks);
        Artemis.LOGGER.info("[Border] display '{}' ({}, {}) -> ({}, {}) color={} anim={}",
            id, bounds[0], bounds[1], bounds[2], bounds[3], String.format("#%06X", color & 0xFFFFFF),
            canShrinkOrExpand && durationTicks > 0);
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
            BorderState.remove(id);
            Artemis.LOGGER.info("[Border] remove '{}'", id);
        }
    }

    private static void onReset(Any message) {
        BorderState.resetAll();
        Artemis.LOGGER.info("[Border] reset");
    }

    private static double[] parseCuboid2D(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        double minX = 0.0D;
        double minZ = 0.0D;
        double maxX = 0.0D;
        double maxZ = 0.0D;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    minX = in.readDouble();
                    break;
                case 2:
                    minZ = in.readDouble();
                    break;
                case 3:
                    maxX = in.readDouble();
                    break;
                case 4:
                    maxZ = in.readDouble();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new double[] {minX, minZ, maxX, maxZ};
    }
}
