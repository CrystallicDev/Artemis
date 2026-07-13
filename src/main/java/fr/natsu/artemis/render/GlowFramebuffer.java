package fr.natsu.artemis.render;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.shader.Framebuffer;

/**
 * Dedicated framebuffer the glowing players' silhouettes are rendered into, before {@link OutlineShader}
 * processes them. A thin wrapper around the vanilla {@link Framebuffer}.
 */
public final class GlowFramebuffer {

    private final Framebuffer framebuffer;

    public GlowFramebuffer(int width, int height) {
        this.framebuffer = new Framebuffer(width, height, true);
        this.framebuffer.setFramebufferColor(0.0F, 0.0F, 0.0F, 0.0F);
        this.framebuffer.setFramebufferFilter(GL11.GL_LINEAR);
    }

    public void bindFramebuffer() {
        // false: we set the viewport ourselves to match the FBO size.
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
