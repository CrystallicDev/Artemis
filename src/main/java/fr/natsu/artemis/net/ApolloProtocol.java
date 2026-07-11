package fr.natsu.artemis.net;

/**
 * Constantes du protocole Apollo (Lunar Client).
 *
 * <p>Apollo communique avec le client sur un canal plugin-message vanilla. Chaque message est un
 * {@code google.protobuf.Any} sérialisé, transporté tel quel dans le payload du packet.</p>
 */
public final class ApolloProtocol {

    /** Canal plugin-message sur lequel transite tout le trafic Apollo. */
    public static final String CHANNEL = "lunar:apollo";

    /**
     * Canal vanilla d'enregistrement des canaux plugin-message. En annonçant {@link #CHANNEL} sur
     * ce canal au login, le serveur nous détecte comme un client Lunar (PlayerRegisterChannelEvent)
     * et commence à pousser les packets Apollo.
     */
    public static final String REGISTER_CHANNEL = "REGISTER";

    /**
     * Préfixe des {@code type_url} protobuf. Un {@code Any} expose son type sous la forme
     * {@code type.googleapis.com/com.lunarclient.apollo.glow.v1.OverrideGlowEffectMessage} ; on ne
     * conserve que la partie après le dernier {@code '/'} comme clé de dispatch.
     */
    public static final String TYPE_URL_PREFIX = "type.googleapis.com/";

    private ApolloProtocol() {
    }

    /**
     * Extrait le nom de type protobuf complet d'un {@code type_url}.
     *
     * @param typeUrl le {@code type_url} d'un {@code Any}
     * @return le nom de message pleinement qualifié (ex. {@code com.lunarclient.apollo.glow.v1.OverrideGlowEffectMessage})
     */
    public static String messageType(String typeUrl) {
        int slash = typeUrl.lastIndexOf('/');
        return slash == -1 ? typeUrl : typeUrl.substring(slash + 1);
    }
}
