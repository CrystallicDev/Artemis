package fr.natsu.artemis.module.hologram;

import java.util.ArrayList;
import java.util.List;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.module.nametag.AdventureText;
import fr.natsu.artemis.module.nametag.AdventureText.Run;
import fr.natsu.artemis.net.ApolloNetwork;

public final class HologramModule {

    private static final String DISPLAY = "lunarclient.apollo.hologram.v1.DisplayHologramMessage";
    private static final String REMOVE = "lunarclient.apollo.hologram.v1.RemoveHologramMessage";
    private static final String RESET = "lunarclient.apollo.hologram.v1.ResetHologramsMessage";

    private HologramModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, HologramModule::onDisplay);
        network.register(REMOVE, HologramModule::onRemove);
        network.register(RESET, HologramModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String id = null;
        double[] loc = null;
        boolean showThroughWalls = false;
        boolean showShadow = false;
        boolean showBackground = false;
        List<List<Run>> lines = new ArrayList<>();

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    id = in.readString();
                    break;
                case 2:
                    loc = parseLocation(in.readBytes());
                    break;
                case 4:
                    showThroughWalls = in.readBool();
                    break;
                case 5:
                    showShadow = in.readBool();
                    break;
                case 6:
                    showBackground = in.readBool();
                    break;
                case 7: {
                    List<Run> runs = AdventureText.parse(in.readString());
                    if (!runs.isEmpty()) {
                        lines.add(runs);
                    }
                    break;
                }
                default:
                    in.skipField(tag);
            }
        }

        if (id == null || loc == null || lines.isEmpty()) {
            return;
        }
        HologramState.display(id, new HologramState.Hologram(
            loc[0], loc[1], loc[2], lines, showThroughWalls, showShadow, showBackground));
        Artemis.LOGGER.info("[Hologram] display '{}' ({}, {}, {}) lines={}", id, loc[0], loc[1], loc[2], lines.size());
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
            HologramState.remove(id);
            Artemis.LOGGER.info("[Hologram] remove '{}'", id);
        }
    }

    private static void onReset(Any message) {
        HologramState.resetAll();
        Artemis.LOGGER.info("[Hologram] reset");
    }

    private static double[] parseLocation(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        double x = 0.0D;
        double y = 0.0D;
        double z = 0.0D;
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
