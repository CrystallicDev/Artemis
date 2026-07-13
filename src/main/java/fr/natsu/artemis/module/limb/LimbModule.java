package fr.natsu.artemis.module.limb;

import java.util.UUID;

import com.google.protobuf.Any;
import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.net.ApolloProtos;

/**
 * Limb module: turns {@code lunarclient.apollo.limb.v1.*} into a hidden-parts mask in
 * {@link LimbState}. The rendering (parts not drawn) is done by {@code MixinModelPlayer}.
 *
 * <p>{@code HideBodyPartMessage} / {@code ResetBodyPartMessage}: player_uuid(#1),
 * body_parts(#2 repeated enum {@code BodyPart}, packed).</p>
 */
public final class LimbModule {

    private static final String HIDE = "lunarclient.apollo.limb.v1.HideBodyPartMessage";
    private static final String RESET = "lunarclient.apollo.limb.v1.ResetBodyPartMessage";
    private static final String HIDE_ARMOR = "lunarclient.apollo.limb.v1.HideArmorPiecesMessage";
    private static final String RESET_ARMOR = "lunarclient.apollo.limb.v1.ResetArmorPiecesMessage";

    private LimbModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(HIDE, LimbModule::onHide);
        network.register(RESET, LimbModule::onReset);
        network.register(HIDE_ARMOR, LimbModule::onHideArmor);
        network.register(RESET_ARMOR, LimbModule::onResetArmor);
    }

    private static void onHideArmor(Any message) throws Exception {
        long[] parsed = parse(message);
        UUID player = uuid(parsed);
        if (player != null) {
            LimbState.hideArmor(player, (int) parsed[2]);
            Artemis.LOGGER.info("[Limb] hide armor {} mask={}", player, Integer.toBinaryString((int) parsed[2]));
        }
    }

    private static void onResetArmor(Any message) throws Exception {
        long[] parsed = parse(message);
        UUID player = uuid(parsed);
        if (player != null) {
            LimbState.resetArmor(player, (int) parsed[2]);
            Artemis.LOGGER.info("[Limb] reset armor {} mask={}", player, Integer.toBinaryString((int) parsed[2]));
        }
    }

    private static void onHide(Any message) throws Exception {
        long[] parsed = parse(message);
        UUID player = uuid(parsed);
        if (player != null) {
            LimbState.hide(player, (int) parsed[2]);
            Artemis.LOGGER.info("[Limb] hide {} mask={}", player, Integer.toBinaryString((int) parsed[2]));
        }
    }

    private static void onReset(Any message) throws Exception {
        long[] parsed = parse(message);
        UUID player = uuid(parsed);
        if (player != null) {
            LimbState.reset(player, (int) parsed[2]);
            Artemis.LOGGER.info("[Limb] reset {} mask={}", player, Integer.toBinaryString((int) parsed[2]));
        }
    }

    /** Parses player_uuid(#1) + body_parts(#2 packed enum) -> [uuidHigh, uuidLow, mask, hasUuid]. */
    private static long[] parse(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        UUID player = null;
        int mask = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            int field = WireFormat.getTagFieldNumber(tag);
            if (field == 1) {
                player = ApolloProtos.parseUuid(in.readBytes());
            } else if (field == 2) {
                if (WireFormat.getTagWireType(tag) == WireFormat.WIRETYPE_LENGTH_DELIMITED) {
                    // Repeated packed.
                    ByteString packed = in.readBytes();
                    CodedInputStream sub = packed.newCodedInput();
                    while (!sub.isAtEnd()) {
                        mask |= LimbState.bit(sub.readInt32());
                    }
                } else {
                    mask |= LimbState.bit(in.readInt32());
                }
            } else {
                in.skipField(tag);
            }
        }
        return new long[] {
            player == null ? 0 : player.getMostSignificantBits(),
            player == null ? 0 : player.getLeastSignificantBits(),
            mask,
            player == null ? 0 : 1
        };
    }

    private static UUID uuid(long[] parsed) {
        return parsed[3] == 0 ? null : new UUID(parsed[0], parsed[1]);
    }
}
