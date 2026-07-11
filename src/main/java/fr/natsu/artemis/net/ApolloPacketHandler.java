package fr.natsu.artemis.net;

import com.google.protobuf.Any;

/**
 * Traite un message Apollo d'un type donné.
 *
 * <p>Un handler reçoit le {@link Any} brut et est responsable de le déballer vers le message
 * protobuf concret, ex. {@code message.unpack(OverrideGlowEffectMessage.class)}.</p>
 *
 * <p><b>Threading</b> : les handlers sont invoqués sur le thread réseau (Netty). Toute interaction
 * avec le jeu (monde, rendu, entités) doit être reportée sur le thread principal via
 * {@code Minecraft.getMinecraft().addScheduledTask(...)}.</p>
 */
@FunctionalInterface
public interface ApolloPacketHandler {

    /**
     * Traite un message Apollo.
     *
     * @param message le message enveloppé dans un {@link Any}
     * @throws Exception si le déballage ou le traitement échoue (loggé par le dispatcher)
     */
    void handle(Any message) throws Exception;
}
