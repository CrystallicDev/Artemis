package fr.natsu.artemis.gui;

/**
 * A HUD element that can be dragged around the config screen: its position (a screen fraction) is
 * read from / written to the config, and it knows how to draw a preview of itself.
 */
public abstract class HudElement {

    public abstract String name();

    public abstract float getX();

    public abstract void setX(float value);

    public abstract float getY();

    public abstract void setY(float value);

    /** Half-width/height of the drag hitbox around the anchor, in scaled pixels. */
    public abstract int halfWidth();

    public abstract int halfHeight();

    /** Draws the preview centered on the anchor (centerX, centerY). */
    public abstract void renderPreview(int centerX, int centerY);
}
