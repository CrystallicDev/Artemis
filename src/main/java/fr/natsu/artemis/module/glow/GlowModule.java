package fr.natsu.artemis.module.glow;

import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Glow module: turns the {@code com.lunarclient.apollo.glow.v1.*} messages into {@link GlowState}
 * updates. The rendering is handled separately by {@code GlowRenderer}.
 *
 * <p>Handled messages:</p>
 * <ul>
 *   <li>{@code OverrideGlowEffectMessage}: {@code player_uuid} (#1), {@code color} (#2, optional)</li>
 *   <li>{@code ResetGlowEffectMessage}: {@code player_uuid} (#1)</li>
 *   <li>{@code ResetGlowEffectsMessage}: empty (resets everything)</li>
 * </ul>
 */
public final class GlowModule {

    // Note: the protobuf type_url uses the *proto* package (lunarclient.apollo.*),
    // NOT the Java package (com.lunarclient.apollo.*).
    private static final String OVERRIDE = "lunarclient.apollo.glow.v1.OverrideGlowEffectMessage";
    private static final String RESET = "lunarclient.apollo.glow.v1.ResetGlowEffectMessage";
    private static final String RESET_ALL = "lunarclient.apollo.glow.v1.ResetGlowEffectsMessage";

    private GlowModule() {
    }

    /** Registers the module's handlers on the network layer. */
    public static void register(ApolloNetwork network) {
        network.register(OVERRIDE, GlowModule::onOverride);
        network.register(RESET, GlowModule::onReset);
        network.register(RESET_ALL, GlowModule::onResetAll);
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

        if (player != null) {
            GlowState.override(player, argb);
            Artemis.LOGGER.info("[Glow] override {} color={}", player,
                argb == null ? "default" : String.format("#%06X", argb & 0xFFFFFF));
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
            GlowState.reset(player);
            Artemis.LOGGER.info("[Glow] reset {}", player);
        }
    }

    private static void onResetAll(Any message) {
        GlowState.resetAll();
        Artemis.LOGGER.info("[Glow] reset all");
    }
}
