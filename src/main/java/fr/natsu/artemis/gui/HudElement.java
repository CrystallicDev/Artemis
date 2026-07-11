package fr.natsu.artemis.gui;

/**
 * Un élément de HUD déplaçable dans l'écran de configuration : sa position (fraction d'écran) est
 * lue/écrite dans la config, et il sait dessiner un aperçu de lui-même.
 */
public abstract class HudElement {

    public abstract String name();

    public abstract float getX();

    public abstract void setX(float value);

    public abstract float getY();

    public abstract void setY(float value);

    /** Demi-largeur/hauteur de la zone de saisie (drag) autour de l'ancre, en pixels scaled. */
    public abstract int halfWidth();

    public abstract int halfHeight();

    /** Dessine l'aperçu centré sur l'ancre (centerX, centerY). */
    public abstract void renderPreview(int centerX, int centerY);
}
