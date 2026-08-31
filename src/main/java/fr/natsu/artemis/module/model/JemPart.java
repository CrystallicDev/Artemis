package fr.natsu.artemis.module.model;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class JemPart {
    private static final float SCALE = 0.0625F;
    private static final float RAD_TO_DEG = 57.295776F;

    public String id = "";
    public String attachTo = null;
    public boolean attach = false;

    public float translateX;
    public float translateY;
    public float translateZ;
    public float rotateX;
    public float rotateY;
    public float rotateZ;
    public float scale = 1.0F;

    public float animTranslateX;
    public float animTranslateY;
    public float animTranslateZ;
    public float animRotateX;
    public float animRotateY;
    public float animRotateZ;
    public boolean hidden;
    public boolean visible = true;
    public float animScaleX = 1.0F;
    public float animScaleY = 1.0F;
    public float animScaleZ = 1.0F;

    public ResourceLocation texture;

    private final List<JemBox> boxes = new ArrayList<>();
    private final List<JemPart> children = new ArrayList<>();

    private int displayList = -1;

    public void addBox(JemBox box) {
        this.boxes.add(box);
    }

    public void addChild(JemPart child) {
        this.children.add(child);
    }

    public List<JemPart> getChildren() {
        return this.children;
    }

    public void resetAnimation() {
        this.animTranslateX = 0.0F;
        this.animTranslateY = 0.0F;
        this.animTranslateZ = 0.0F;
        this.animRotateX = 0.0F;
        this.animRotateY = 0.0F;
        this.animRotateZ = 0.0F;
        this.hidden = false;
        this.animScaleX = 1.0F;
        this.animScaleY = 1.0F;
        this.animScaleZ = 1.0F;
        for (JemPart child : this.children) {
            child.resetAnimation();
        }
    }

    public JemPart find(String name) {
        if (name.equals(this.id)) {
            return this;
        }
        for (JemPart child : this.children) {
            JemPart found = child.find(name);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void render() {
        if (this.hidden || !this.visible) {
            return;
        }

        GL11.glPushMatrix();
        GL11.glTranslatef((this.translateX + this.animTranslateX) * SCALE,
            (this.translateY + this.animTranslateY) * SCALE,
            (this.translateZ + this.animTranslateZ) * SCALE);

        float rz = this.rotateZ + this.animRotateZ;
        float ry = this.rotateY + this.animRotateY;
        float rx = this.rotateX + this.animRotateX;
        if (rz != 0.0F) {
            GL11.glRotatef(rz * RAD_TO_DEG, 0.0F, 0.0F, 1.0F);
        }
        if (ry != 0.0F) {
            GL11.glRotatef(ry * RAD_TO_DEG, 0.0F, 1.0F, 0.0F);
        }
        if (rx != 0.0F) {
            GL11.glRotatef(rx * RAD_TO_DEG, 1.0F, 0.0F, 0.0F);
        }
        float scaleX = this.scale * this.animScaleX;
        float scaleY = this.scale * this.animScaleY;
        float scaleZ = this.scale * this.animScaleZ;
        if (scaleX != 1.0F || scaleY != 1.0F || scaleZ != 1.0F) {
            GL11.glScalef(scaleX, scaleY, scaleZ);
        }

        if (!this.boxes.isEmpty()) {
            if (this.texture != null) {
                Minecraft.getMinecraft().getTextureManager().bindTexture(this.texture);
            }
            if (this.displayList < 0) {
                this.compile();
            }
            GL11.glCallList(this.displayList);
        }

        for (JemPart child : this.children) {
            child.render();
        }

        GL11.glPopMatrix();
    }

    private void compile() {
        this.displayList = GL11.glGenLists(1);
        GL11.glNewList(this.displayList, GL11.GL_COMPILE);
        GL11.glBegin(GL11.GL_QUADS);
        for (JemBox box : this.boxes) {
            box.render();
        }
        GL11.glEnd();
        GL11.glEndList();
    }

    public void dispose() {
        if (this.displayList >= 0) {
            GL11.glDeleteLists(this.displayList, 1);
            this.displayList = -1;
        }
        for (JemPart child : this.children) {
            child.dispose();
        }
    }
}
