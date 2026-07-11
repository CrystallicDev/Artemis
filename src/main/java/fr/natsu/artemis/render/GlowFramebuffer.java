package fr.natsu.artemis.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

/**
 * Framebuffer dédié dans lequel les silhouettes des joueurs en surbrillance sont rendues, avant
 * traitement par {@link OutlineShader}. Wrapper léger autour du {@link Framebuffer} vanilla.
 */
public final class GlowFramebuffer {

    private final Framebuffer framebuffer;

    public GlowFramebuffer(int width, int height) {
        this.framebuffer = new Framebuffer(width, height, true);
        this.framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
    }

    public void bindFramebuffer() {
        // false : on gère le viewport manuellement pour coller à la taille du FBO.
        this.framebuffer.bindFramebuffer(false);
        GL11.glViewport(0, 0, this.framebuffer.framebufferWidth, this.framebuffer.framebufferHeight);
    }

    public void clear() {
        this.framebuffer.framebufferClear();
    }

    public void unbindFramebuffer() {
        Minecraft.getMinecraft().getFramebuffer().bindFramebuffer(true);
    }

    public int getFramebufferTexture() {
        return this.framebuffer.framebufferTexture;
    }

    public void cleanup() {
        this.framebuffer.deleteFramebuffer();
    }
}
