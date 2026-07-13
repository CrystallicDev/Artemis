package fr.natsu.artemis.module.coloredfire;

import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Colored Fire module: turns {@code lunarclient.apollo.coloredfire.v1.*} into {@link ColoredFireState}
 * colors. The rendering (tinting the flames) is done by {@code MixinRenderFire}.
 *
 * <p>{@code OverrideColoredFireMessage}: player_uuid(#1), color(#2). Reset(uuid), ResetAll(empty).</p>
 */
public final class ColoredFireModule {

    private static final String OVERRIDE = "lunarclient.apollo.coloredfire.v1.OverrideColoredFireMessage";
    private static final String RESET = "lunarclient.apollo.coloredfire.v1.ResetColoredFireMessage";
    private static final String RESET_ALL = "lunarclient.apollo.coloredfire.v1.ResetColoredFiresMessage";

    private ColoredFireModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(OVERRIDE, ColoredFireModule::onOverride);
        network.register(RESET, ColoredFireModule::onReset);
        network.register(RESET_ALL, ColoredFireModule::onResetAll);
    }

    private static void onOverride(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        UUID player = null;
        Integer argb = null;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    player = ApolloProtos.parseUuid(in.readBytes());
                    break;
                case 2:
                    argb = ApolloProtos.parseColorArgb(in.readBytes());
                    break;
                default:
                    in.skipField(tag);
            }
        }
        if (player != null && argb != null) {
            ColoredFireState.override(player, argb);
            Artemis.LOGGER.info("[ColoredFire] override {} #{}", player, String.format("%06X", argb & 0xFFFFFF));
        }
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
            ColoredFireState.reset(player);
            Artemis.LOGGER.info("[ColoredFire] reset {}", player);
        }
    }

    private static void onResetAll(Any message) {
        ColoredFireState.resetAll();
        Artemis.LOGGER.info("[ColoredFire] reset all");
    }
}
