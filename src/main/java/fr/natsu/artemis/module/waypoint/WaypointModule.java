package fr.natsu.artemis.module.waypoint;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Waypoint module: turns {@code lunarclient.apollo.waypoint.v1.*} into {@link WaypointState}. The
 * rendering (beam + label) is done by {@code WaypointRenderer}.
 *
 * <p>{@code DisplayWaypointMessage}: name(#1 str), location(#2 BlockLocation), color(#3 Color),
 * prevent_removal(#4 bool), hidden(#5 bool), show_beam(#6 bool), highlight_block(#7 bool),
 * highlight_block_line_width(#8 float), style(#9). BlockLocation x/y/z are sint32 (zigzag).</p>
 */
public final class WaypointModule {

    private static final String DISPLAY = "lunarclient.apollo.waypoint.v1.DisplayWaypointMessage";
    private static final String REMOVE = "lunarclient.apollo.waypoint.v1.RemoveWaypointMessage";
    private static final String RESET = "lunarclient.apollo.waypoint.v1.ResetWaypointsMessage";
    private static final String SHOW = "lunarclient.apollo.waypoint.v1.ShowWaypointMessage";
    private static final String HIDE = "lunarclient.apollo.waypoint.v1.HideWaypointMessage";

    private static final int DEFAULT_COLOR = 0xFFFFFFFF;

    private WaypointModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, WaypointModule::onDisplay);
        network.register(REMOVE, WaypointModule::onRemove);
        network.register(RESET, WaypointModule::onReset);
        network.register(SHOW, WaypointModule::onShow);
        network.register(HIDE, WaypointModule::onHide);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String name = null;
        int[] loc = null;
        int color = DEFAULT_COLOR;
        boolean hidden = false;
        boolean showBeam = false;
        boolean highlightBlock = false;
        float lineWidth = 0.0F;
        // Defaults when no style is sent: show both text and distance.
        boolean[] style = {true, true};

        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    name = in.readString();
                    break;
                case 2:
                    loc = parseBlockLocation(in.readBytes());
                    break;
                case 3:
                    color = ApolloProtos.parseColorArgb(in.readBytes());
                    break;
                case 5:
                    hidden = in.readBool();
                    break;
                case 6:
                    showBeam = in.readBool();
                    break;
                case 7:
                    highlightBlock = in.readBool();
                    break;
                case 8:
                    lineWidth = in.readFloat();
                    break;
                case 9:
                    style = parseStyle(in.readBytes());
                    break;
                default:
                    // #4 prevent_removal: ignored.
                    in.skipField(tag);
            }
        }

        if (name == null || loc == null) {
            return;
        }
        WaypointState.display(new WaypointState.Waypoint(
            name, loc[0], loc[1], loc[2], color, showBeam, highlightBlock, lineWidth,
            style[0], style[1], hidden));
        Artemis.LOGGER.info("[Waypoint] display '{}' ({}, {}, {}) beam={}", name, loc[0], loc[1], loc[2], showBeam);
    }

    private static void onRemove(Any message) throws Exception {
        String name = readName(message);
        if (name != null) {
            WaypointState.remove(name);
            Artemis.LOGGER.info("[Waypoint] remove '{}'", name);
        }
    }

    private static void onReset(Any message) {
        WaypointState.resetAll();
        Artemis.LOGGER.info("[Waypoint] reset");
    }

    private static void onShow(Any message) throws Exception {
        String name = readName(message);
        if (name != null) {
            WaypointState.setHidden(name, false);
        }
    }

    private static void onHide(Any message) throws Exception {
        String name = readName(message);
        if (name != null) {
            WaypointState.setHidden(name, true);
        }
    }

    /** Reads the name(#1 string) field of a simple message (Remove/Show/Hide). */
    private static String readName(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String name = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                name = in.readString();
            } else {
                in.skipField(tag);
            }
        }
        return name;
    }

    /** Parses a {@code WaypointTextStyle}: show_text(#1), show_distance(#9). Returns [showText, showDistance]. */
    private static boolean[] parseStyle(ByteString data) throws Exception {
        CodedInputStream in = data.newCodedInput();
        boolean showText = true;
        boolean showDistance = true;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    showText = in.readBool();
                    break;
                case 9:
                    showDistance = in.readBool();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new boolean[] {showText, showDistance};
    }

    /** Parses a {@code BlockLocation}: world(#1 str, ignored), x(#2), y(#3), z(#4) as sint32. */
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
