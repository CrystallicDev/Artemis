package fr.natsu.artemis.module.lightning;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

import fr.natsu.artemis.Artemis;

/**
 * Éclairs colorés Artemis : réception des ordres d'apparition sur le canal dédié {@link #CHANNEL}
 * (feature hors Apollo, comme le chat). Le rendu est fait par {@code LightningRenderer}.
 *
 * <p>Payload (lu en {@link DataInputStream}, big-endian) : {@code double x}, {@code double y},
 * {@code double z}, {@code int mainColor} (ARGB, glow externe), {@code int coreColor} (ARGB, cœur).</p>
 */
public final class ArtemisLightning {

    /** Canal dédié. Son enregistrement signale aussi au serveur un client Artemis. */
    public static final String CHANNEL = "artemis:lightning";

    private ArtemisLightning() {
    }

    /** Traite un payload reçu sur {@link #CHANNEL} (thread Netty). */
    public static void handleIncoming(byte[] payload) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(payload))) {
            double x = in.readDouble();
            double y = in.readDouble();
            double z = in.readDouble();
            int mainColor = in.readInt();
            int coreColor = in.readInt();
            LightningState.add(x, y, z, mainColor, coreColor);
        } catch (Exception e) {
            Artemis.LOGGER.warn("[Lightning] payload illisible : {}", e.toString());
        }
    }
}
