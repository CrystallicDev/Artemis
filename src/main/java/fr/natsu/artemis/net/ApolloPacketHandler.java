package fr.natsu.artemis.net;

import com.google.protobuf.Any;

/**
 * Handles an Apollo message of a given type.
 *
 * <p>A handler receives the raw {@link Any} and is responsible for unpacking it into the concrete
 * protobuf message, e.g. {@code message.unpack(OverrideGlowEffectMessage.class)}.</p>
 *
 * <p><b>Threading</b>: handlers are called on the network thread (Netty). Any interaction with the
 * game (world, rendering, entities) must be deferred to the main thread through
 * {@code Minecraft.getMinecraft().addScheduledTask(...)}.</p>
 */
@FunctionalInterface
public interface ApolloPacketHandler {

    /**
     * Handles an Apollo message.
     *
     * @param message the message wrapped in an {@link Any}
     * @throws Exception if unpacking or handling fails (logged by the dispatcher)
     */
    void handle(Any message) throws Exception;
}
