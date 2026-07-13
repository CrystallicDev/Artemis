package fr.natsu.artemis;

/**
 * Runtime settings shared across the Artemis modules.
 */
public final class ArtemisSettings {

    /**
     * When true, Apollo effects that target the local client (glow, nametag, ...) also show up on the
     * player themselves. Handy for testing without a second account.
     *
     * <p>Note: in first-person view the player model isn't rendered normally, so the self-glow is only
     * actually visible in third-person view (F5).</p>
     */
    public static boolean renderSelf = true;

    private ArtemisSettings() {
    }
}
