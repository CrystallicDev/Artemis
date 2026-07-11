package fr.natsu.artemis;

/**
 * Réglages runtime partagés entre les modules Artemis.
 */
public final class ArtemisSettings {

    /**
     * Si vrai, les effets Apollo ciblant le client local (glow, nametag, ...) s'affichent aussi sur
     * le joueur lui-même. Pratique pour tester sans deuxième compte.
     *
     * <p>NB : en vue 1ʳᵉ personne, le modèle du joueur n'est pas rendu normalement — le self-glow
     * n'est donc réellement visible qu'en vue 3ᵉ personne (F5).</p>
     */
    public static boolean renderSelf = true;

    private ArtemisSettings() {
    }
}
