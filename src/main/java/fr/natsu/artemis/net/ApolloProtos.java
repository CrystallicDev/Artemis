package fr.natsu.artemis.net;

import java.io.IOException;
import java.util.UUID;

import com.google.protobuf.ByteString;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

/**
 * Manual protobuf readers for the common Apollo types.
 *
 * <p>The simple Apollo messages are decoded by hand through {@link CodedInputStream} rather than with
 * the generated classes: no descriptor-bootstrap dependency, and forward-compat tolerance of unknown
 * fields through {@link CodedInputStream#skipField(int)}.</p>
 *
 * <p>Encodings (see {@code com.lunarclient.apollo.common.v1}):</p>
 * <ul>
 *   <li>{@code Uuid}: {@code high64} (#1, fixed64) + {@code low64} (#2, fixed64)</li>
 *   <li>{@code Color}: {@code color} (#1, int32) in ARGB ({@code java.awt.Color#getRGB()})</li>
 * </ul>
 */
public final class ApolloProtos {

    private ApolloProtos() {
    }

    /**
     * Decodes a {@code Uuid} message.
     *
     * @param data the serialized message content
     * @return the rebuilt UUID
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
     * Decodes a {@code google.protobuf.Duration} ({@code seconds} #1 int64, {@code nanos} #2 int32)
     * into milliseconds.
     *
     * @param data the serialized message content
     * @return the duration in milliseconds
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
     * Decodes a {@code Color} message.
     *
     * @param data the serialized message content
     * @return the ARGB value
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
