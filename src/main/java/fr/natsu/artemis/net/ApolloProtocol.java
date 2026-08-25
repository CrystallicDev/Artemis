package fr.natsu.artemis.net;

/**
 * Apollo (Lunar Client) protocol constants.
 *
 * <p>Apollo talks to the client over a vanilla plugin-message channel. Every message is a serialized
 * {@code google.protobuf.Any}, carried as-is inside the packet payload.</p>
 */
public final class ApolloProtocol {

    public static final String CHANNEL = "lunar:apollo";

    public static final String REGISTER_CHANNEL = "REGISTER";

    public static final String TYPE_URL_PREFIX = "type.googleapis.com/";

    private ApolloProtocol() {
    }

    public static String messageType(String typeUrl) {
        int slash = typeUrl.lastIndexOf('/');
        return slash == -1 ? typeUrl : typeUrl.substring(slash + 1);
    }
}
