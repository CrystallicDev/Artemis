package fr.natsu.artemis.render;

import fr.natsu.artemis.LunarHost;
import org.lwjgl.BufferUtils;
import java.nio.IntBuffer;
import java.nio.ByteBuffer;

import org.lwjgl.opengl.GL11;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.texture.TextureUtil;

public final class GlowFramebuffer {

    private static final int GL_DEPTH_COMPONENT24 = 33190;
    private static final int GL_FRAMEBUFFER_BINDING = 36006;

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

    private int previousFramebuffer = -1;
    private final int[] previousViewport = new int[4];

    public void bindFramebuffer() {
        if (LunarHost.isLunar()) {
            this.previousFramebuffer = GL11.glGetInteger(GL_FRAMEBUFFER_BINDING);
            IntBuffer viewport = BufferUtils.createIntBuffer(16);
            GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);
            viewport.get(this.previousViewport, 0, 4);
        }
        OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.framebufferObject);
        GL11.glViewport(0, 0, this.width, this.height);
        // TOUJOURS via GlStateManager : un glClearColor brut desynchronise son cache ; au frame suivant
        // GlStateManager.clearColor(couleur du ciel) est saute alors que GL est a noir -> ciel qui clignote.
        GlStateManager.viewport(0, 0, this.width, this.height);
        GlStateManager.clearColor(0.0F, 0.0F, 0.0F, 0.0F);
        // Le clear lui-meme reste brut : sous OptiFine FastRender, GlStateManager.clear est un no-op pendant le rendu
        // (clearEnabled=false) et le FBO ne serait plus jamais vide -> contours qui s'accumulent sur tout l'ecran.
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
    }

    public void unbindFramebuffer() {
        if (this.previousFramebuffer >= 0) {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, this.previousFramebuffer);
            GL11.glViewport(this.previousViewport[0], this.previousViewport[1],
                this.previousViewport[2], this.previousViewport[3]);
            this.previousFramebuffer = -1;
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (OpenGlHelper.isFramebufferEnabled()) {
            mc.getFramebuffer().bindFramebuffer(true);
        } else {
            OpenGlHelper.glBindFramebuffer(OpenGlHelper.GL_FRAMEBUFFER, 0);
            GlStateManager.viewport(0, 0, mc.displayWidth, mc.displayHeight);
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
