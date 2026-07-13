package fr.natsu.artemis.module.lightning;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import fr.natsu.artemis.Artemis;

/**
 * Artemis colored lightning: receives spawn orders on its own channel {@link #CHANNEL} (a non-Apollo
 * feature, like the chat). The actual drawing is done by {@code LightningRenderer}.
 *
 * <p>Payload (read with a {@link DataInputStream}, big-endian): {@code double x}, {@code double y},
 * {@code double z}, {@code int mainColor} (ARGB, outer glow), {@code int coreColor} (ARGB, core).</p>
 */
public final class ArtemisLightning {

    /** Dedicated channel. Registering it also tells the server this is an Artemis client. */
    public static final String CHANNEL = "artemis:lightning";

    private ArtemisLightning() {
    }

    /** Handles a payload received on {@link #CHANNEL} (Netty thread). */
    public static void handleIncoming(byte[] payload) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            int mainColor = in.readInt();
            int coreColor = in.readInt();
            LightningState.add(x, y, z, mainColor, coreColor);
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Lightning] unreadable payload: {}", e.toString());
        }
    }
}
