package fr.natsu.artemis.module.model;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;

public final class JemModel {
    public ResourceLocation texture;
    public float textureWidth = 64.0F;
    public float textureHeight = 32.0F;
    public float shadowSize = 1.0F;

    private final List<JemPart> parts = new ArrayList<>();

    public void addPart(JemPart part) {
        this.parts.add(part);
    }

    public List<JemPart> getParts() {
        return this.parts;
    }

    public JemPart find(String id) {
        for (JemPart part : this.parts) {
            JemPart found = part.find(id);
            if (found != null) {
                return found;
            }
        }
        return null;
    }

    public void resetAnimation() {
        for (JemPart part : this.parts) {
            part.resetAnimation();
        }
    }

    public void render() {
        if (this.texture != null) {
            Minecraft.getMinecraft().getTextureManager().bindTexture(this.texture);
        }
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        for (JemPart part : this.parts) {
            part.render();
        }
    }

    public void dispose() {
        for (JemPart part : this.parts) {
            part.dispose();
        }
    }
}
