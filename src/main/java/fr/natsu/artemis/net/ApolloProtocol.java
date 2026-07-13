package fr.natsu.artemis.net;

/**
 * Apollo (Lunar Client) protocol constants.
 *
 * <p>Apollo talks to the client over a vanilla plugin-message channel. Every message is a serialized
 * {@code google.protobuf.Any}, carried as-is inside the packet payload.</p>
 */
public final class ApolloProtocol {

    /** Plugin-message channel that carries all Apollo traffic. */
    public static final String CHANNEL = "lunar:apollo";

    /**
     * Vanilla channel used to register plugin-message channels. Announcing {@link #CHANNEL} on this
     * channel at login makes the server detect us as a Lunar client (PlayerRegisterChannelEvent) and
     * start pushing Apollo packets.
     */
    public static final String REGISTER_CHANNEL = "REGISTER";

    /**
     * Prefix of protobuf {@code type_url}s. An {@code Any} exposes its type as
     * {@code type.googleapis.com/com.lunarclient.apollo.glow.v1.OverrideGlowEffectMessage}; we only
     * keep the part after the last {@code '/'} as the dispatch key.
     */
    public static final String TYPE_URL_PREFIX = "type.googleapis.com/";

    private ApolloProtocol() {
    }

    /**
     * Extracts the full protobuf type name from a {@code type_url}.
     *
     * @param typeUrl the {@code type_url} of an {@code Any}
     * @return the fully-qualified message name (e.g. {@code com.lunarclient.apollo.glow.v1.OverrideGlowEffectMessage})
     */
    public static String messageType(String typeUrl) {
        int slash = typeUrl.lastIndexOf('/');
        return slash == -1 ? typeUrl : typeUrl.substring(slash + 1);
    }
}
