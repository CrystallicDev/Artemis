package fr.natsu.artemis.render;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.ArtemisSettings;
import fr.natsu.artemis.compat.OptiFineCompat;
import fr.natsu.artemis.module.glow.GlowState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Rendu du Glowing en deux passes, piloté par {@link GlowState} :
 *
 * <ol>
 *   <li>{@link #renderEntitiesToFramebuffer} re-rend les joueurs en surbrillance comme silhouettes
 *       plates dans un FBO dédié (couleur injectée via {@code MixinRendererLivingEntity} qui lit
 *       {@link GlowState}).</li>
 *   <li>{@link #displayFramebuffer} applique {@link OutlineShader} sur ce FBO pour transformer les
 *       silhouettes en contours composités à l'écran.</li>
 * </ol>
 *
 * <p>Adapté du {@code ShaderManager} d'Eterion. Tout s'exécute sur le thread de rendu.</p>
 */
public final class GlowRenderer {

    private static OutlineShader outlineShader;
    private static GlowFramebuffer outlineFramebuffer;

    /** Entité en cours de rendu dans le FBO ; lue par le mixin de couleur d'outline. */
    public static Entity renderTarget = null;
    /** Vrai pendant la passe de rendu du FBO ; gate les mixins (team placeholder, couleur). */
    public static boolean isRenderingOutline = false;

    private static boolean foundOneToRender = false;
    private static int lastWidth = 0;
    private static int lastHeight = 0;

    private GlowRenderer() {
    }

    /**
     * Initialise (ou réinitialise après resize) le shader et le framebuffer. Doit être appelé avec
     * un contexte GL valide.
     */
    private static void ensureInitialised() {
        Minecraft mc = Minecraft.getMinecraft();

        if (outlineShader == null) {
            try {
                outlineShader = new OutlineShader(
                    "shaders/program/outline_vertex.vsh",
                    "shaders/program/outline_fragment.fsh");
            } catch (Exception e) {
                Artemis.LOGGER.error("[Glow] impossible de charger le shader d'outline", e);
                return;
            }
        }

        if (outlineFramebuffer == null || mc.displayWidth != lastWidth || mc.displayHeight != lastHeight) {
            if (outlineFramebuffer != null) {
                outlineFramebuffer.cleanup();
            }
            outlineFramebuffer = new GlowFramebuffer(mc.displayWidth, mc.displayHeight);
            lastWidth = mc.displayWidth;
            lastHeight = mc.displayHeight;
        }
    }

    private static boolean isReady() {
        return outlineShader != null && outlineFramebuffer != null;
    }

    // ------------------------------------------------------------------
    // Passe 1 : silhouettes -> framebuffer
    // ------------------------------------------------------------------

    public static void renderEntitiesToFramebuffer(Entity renderViewEntity, ICamera camera, float partialTicks) {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || GlowState.isEmpty()) {
            return;
        }
        // Fast Render (OptiFine) casse notre passe FBO + shader (écran noir) : on ne rend pas le
        // Glowing tant qu'il est actif. Testé à chaque frame -> se réactive dès que l'option est coupée.
        if (OptiFineCompat.isFastRender()) {
            return;
        }

        ensureInitialised();
        if (!isReady()) {
            return;
        }

        GlStateManager.depthFunc(519);
        GlStateManager.disableFog();
        outlineFramebuffer.clear();
        outlineFramebuffer.bindFramebuffer();
        RenderHelper.disableStandardItemLighting();
        mc.getRenderManager().setRenderOutlines(true);
        isRenderingOutline = true;

        for (Entity entity : mc.theWorld.loadedEntityList) {
            if (shouldRenderEntity(entity, renderViewEntity)) {
                renderTarget = entity;
                foundOneToRender = true;
                mc.getRenderManager().renderEntitySimple(entity, partialTicks);
            }
        }

        mc.getRenderManager().setRenderOutlines(false);
        isRenderingOutline = false;
        renderTarget = null;
        RenderHelper.enableStandardItemLighting();
        GlStateManager.depthMask(false);

        GlStateManager.enableLighting();
        GlStateManager.depthMask(true);

        outlineFramebuffer.unbindFramebuffer();

        GlStateManager.enableFog();
        GlStateManager.enableBlend();
        GlStateManager.enableColorMaterial();
        GlStateManager.depthFunc(515);
        GlStateManager.enableDepth();
        GlStateManager.enableAlpha();
    }

    private static boolean shouldRenderEntity(Entity entity, Entity renderViewEntity) {
        if (!(entity instanceof EntityPlayer)) {
            return false;
        }
        if (entity == renderViewEntity) {
            // Self-glow : seulement si activé ET en 3e personne. En 1ère personne le modèle joueur n'est
            // pas visible, mais sa silhouette rendue dans le FBO couvre les bords de l'écran -> le shader
            // d'outline (Sobel) détecte une arête sur le bord du framebuffer = fines bandes colorées sur
            // le pourtour de l'écran.
            if (!ArtemisSettings.renderSelf || Minecraft.getMinecraft().gameSettings.thirdPersonView == 0) {
                return false;
            }
        }
        EntityPlayer player = (EntityPlayer) entity;
        if (player.isSpectator() || player.isInvisible()) {
            return false;
        }
        return GlowState.isGlowing(player.getUniqueID());
    }

    // ------------------------------------------------------------------
    // Passe 2 : framebuffer -> contour à l'écran
    // ------------------------------------------------------------------

    public static void displayFramebuffer() {
        if (!foundOneToRender || !isReady() || OptiFineCompat.isFastRender()) {
            foundOneToRender = false;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();

        GL11.glPushAttrib(GL11.GL_ALL_ATTRIB_BITS);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glLoadIdentity();
        GL11.glOrtho(0, mc.displayWidth, mc.displayHeight, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_FOG);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        outlineShader.bind();
        GL20.glUniform1i(outlineShader.getUniform("texture"), 0);
        int resolutionUniform = outlineShader.getUniform("resolution");
        if (resolutionUniform != -1) {
            GL20.glUniform2f(resolutionUniform, (float) mc.displayWidth, (float) mc.displayHeight);
        }

        renderFullscreenQuad(outlineFramebuffer.getFramebufferTexture());

        outlineShader.unbind();

        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPopMatrix();
        GL11.glPopAttrib();

        foundOneToRender = false;
    }

    private static void renderFullscreenQuad(int textureId) {
        Minecraft mc = Minecraft.getMinecraft();
        int width = mc.displayWidth;
        int height = mc.displayHeight;

        GL11.glViewport(0, 0, width, height);
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();
        GL11.glOrtho(0, width, height, 0, -1, 1);
        GL11.glMatrixMode(GL11.GL_MODELVIEW);
        GL11.glPushMatrix();
        GL11.glLoadIdentity();

        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glDisable(GL11.GL_CULL_FACE);
        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_ALPHA_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL13.glActiveTexture(GL13.GL_TEXTURE0);
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, textureId);

        GL11.glColor4f(1, 1, 1, 1);

        GL11.glBegin(GL11.GL_QUADS);
        GL11.glTexCoord2f(0, 1);
        GL11.glVertex2f(0, 0);
        GL11.glTexCoord2f(1, 1);
        GL11.glVertex2f(width, 0);
        GL11.glTexCoord2f(1, 0);
        GL11.glVertex2f(width, height);
        GL11.glTexCoord2f(0, 0);
        GL11.glVertex2f(0, height);
        GL11.glEnd();

        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_PROJECTION);
        GL11.glPopMatrix();
        GL11.glMatrixMode(GL11.GL_MODELVIEW);

        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }
}
