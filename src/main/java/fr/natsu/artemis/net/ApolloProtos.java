package fr.natsu.artemis.net;

import java.io.IOException;
import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

/**
 * Lecteurs protobuf manuels pour les types communs Apollo.
 *
 * <p>Les messages Apollo simples sont décodés à la main via {@link CodedInputStream} plutôt qu'avec
 * les classes générées : zéro dépendance au bootstrap de descripteurs, et tolérance aux champs
 * inconnus (forward-compat) via {@link CodedInputStream#skipField(int)}.</p>
 *
 * <p>Encodages (cf. {@code com.lunarclient.apollo.common.v1}) :</p>
 * <ul>
 *   <li>{@code Uuid} : {@code high64} (#1, fixed64) + {@code low64} (#2, fixed64)</li>
 *   <li>{@code Color} : {@code color} (#1, int32) au format ARGB ({@code java.awt.Color#getRGB()})</li>
 * </ul>
 */
public final class ApolloProtos {

    private ApolloProtos() {
    }

    /**
     * Décode un message {@code Uuid}.
     *
     * @param data le contenu sérialisé du message
     * @return l'UUID reconstruit
     */
    public static UUID parseUuid(ByteString data) throws IOException {
        CodedInputStream in = data.newCodedInput();
        long high = 0L;
        long low = 0L;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    high = in.readFixed64();
                    break;
                case 2:
                    low = in.readFixed64();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return new UUID(high, low);
    }

    /**
     * Décode un {@code google.protobuf.Duration} ({@code seconds} #1 int64, {@code nanos} #2 int32)
     * en millisecondes.
     *
     * @param data le contenu sérialisé du message
     * @return la durée en millisecondes
     */
    public static long parseDurationMillis(ByteString data) throws IOException {
        CodedInputStream in = data.newCodedInput();
        long seconds = 0L;
        int nanos = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    seconds = in.readInt64();
                    break;
                case 2:
                    nanos = in.readInt32();
                    break;
                default:
                    in.skipField(tag);
            }
        }
        return seconds * 1000L + nanos / 1_000_000L;
    }

    /**
     * Décode un message {@code Color}.
     *
     * @param data le contenu sérialisé du message
     * @return la valeur ARGB
     */
    public static int parseColorArgb(ByteString data) throws IOException {
        CodedInputStream in = data.newCodedInput();
        int argb = 0;
        int tag;
        while ((tag = in.readTag()) != 0) {
            if (WireFormat.getTagFieldNumber(tag) == 1) {
                argb = in.readInt32();
            } else {
                in.skipField(tag);
            }
        }
        return argb;
    }
}
