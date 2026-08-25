package fr.natsu.artemis.render;

import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;

public final class GlowFramebuffer {

    private static final int GL_DEPTH_COMPONENT24 = 33190;

    private final int width;
    private final int height;
    private int framebufferObject = -1;
    private int framebufferTexture = -1;
    private int depthBuffer = -1;

    public GlowFramebuffer(int width, int height) {
        this.width = width;
        this.height = height;
        this.create();
    }

    private void create() {
        this.framebufferObject = OpenGlHelper.glGenFramebuffers();
        this.framebufferTexture = TextureUtil.glGenTextures();
        this.depthBuffer = OpenGlHelper.glGenRenderbuffers();

        GlStateManager.bindTexture(this.framebufferTexture);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 10496.0F);
        GL11.glTexParameterf(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 10496.0F);
        GL11.glTexImage2D(GL11.GL_TEXTURE_2D, 0, GL11.GL_RGBA8, this.width, this.height, 0,
            GL11.GL_RGBA, GL11.GL_UNSIGNED_BYTE, (ByteBuffer) null);

        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
        OpenGlHelper.glFramebufferTexture2D(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_COLOR_ATTACHMENT0,
            GL11.GL_TEXTURE_2D, this.framebufferTexture, 0);

        OpenGlHelper.glBindRenderbuffer(OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);
        OpenGlHelper.glRenderbufferStorage(OpenGlHelper.GL_RENDERBUFFER, GL_DEPTH_COMPONENT24, this.width, this.height);
        OpenGlHelper.glFramebufferRenderbuffer(OpenGlHelper.GL_FRAMEBUFFER, OpenGlHelper.GL_DEPTH_ATTACHMENT,
            OpenGlHelper.GL_RENDERBUFFER, this.depthBuffer);

        GlStateManager.bindTexture(0);
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
    }

    public void bindFramebuffer() {
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
        GL11.glViewport(0, 0, this.width, this.height);
        GL11.glClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void unbindFramebuffer() {
        Minecraft mc = Minecraft.getMinecraft();
        if (OpenGlHelper.isFramebufferEnabled()) {
            mc.getFramebuffer().bindFramebuffer(true);
        } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
        }
    }

    public int getFramebufferTexture() {
        return this.framebufferTexture;
    }

    public void cleanup() {
        if (this.depthBuffer > -1) {
            OpenGlHelper.glDeleteRenderbuffers(this.depthBuffer);
            this.depthBuffer = -1;
        }
        if (this.framebufferTexture > -1) {
            TextureUtil.deleteTexture(this.framebufferTexture);
            this.framebufferTexture = -1;
        }
        if (this.framebufferObject > -1) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            OpenGlHelper.glDeleteFramebuffers(this.framebufferObject);
            this.framebufferObject = -1;
        }
    }
}
