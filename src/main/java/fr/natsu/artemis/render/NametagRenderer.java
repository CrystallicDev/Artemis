package fr.natsu.artemis.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.ArtemisSettings;
import fr.natsu.artemis.module.nametag.AdventureText;
import fr.natsu.artemis.module.nametag.NametagState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

/**
 * Draws the Apollo nametags above players, replacing the vanilla nametag (by cancelling
 * {@code RenderLivingEvent.Specials.Pre}, which wraps {@code renderName}).
 *
 * <p>The billboard transform is taken from Eterion's {@code CustomNametagRenderer}. The lines (from
 * {@link NametagState}) are stacked above the head, each run drawn with its RGB color for full hex
 * support.</p>
 */
public final class NametagRenderer {

    private static final float TEXT_SCALE = 0.02666667F;
    private static final double MAX_RENDER_DIST_SQ = 4096.0D;

    @SubscribeEvent
    public void onRenderName(RenderLivingEvent.Specials.Pre<EntityLivingBase> event) {
        if (!(event.entity instanceof EntityPlayer)) {
            return;
        }
        EntityPlayer player = (EntityPlayer) event.entity;

        List<List<AdventureText.Run>> lines = NametagState.get(player.getUniqueID());
        if (lines == null || lines.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (player == mc.thePlayer && mc.gameSettings.thirdPersonView == 0 && !ArtemisSettings.renderSelf) {
            return;
        }
        if (player.getDistanceSqToEntity(mc.getRenderViewEntity()) > MAX_RENDER_DIST_SQ) {
            return;
        }

        // We take over the nametag: no vanilla render on top of ours.
        event.setCanceled(true);
        render(player, lines, event.x, event.y, event.z);
    }

    private void render(EntityPlayer player, List<List<AdventureText.Run>> lines, double x, double y, double z) {
        Minecraft mc = Minecraft.getMinecraft();
        FontRenderer font = mc.fontRendererObj;

        float baseOffset = player.height + 0.5F;
        int lineHeight = font.FONT_HEIGHT + 1;
        int count = lines.size();

        GL11.glPushMatrix();
        GL11.glTranslated(x, y + baseOffset, z);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glDisable(GL11.GL_DEPTH_TEST);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        for (int i = 0; i < count; i++) {
            List<AdventureText.Run> runs = lines.get(i);

            int width = 0;
            for (AdventureText.Run run : runs) {
                width += font.getStringWidth(run.display);
            }

            // In the vanilla nametag space, a positive font-y goes down (into the skin), negative goes
            // up. We keep everything at font-y <= 0: the last line sits just above the head (font-y = 0)
            // and the earlier ones stack upward.
            int lineY = -i * lineHeight;
            int startX = -width / 2;

            drawBackground(startX - 1, lineY - 1, startX + width + 1, lineY + font.FONT_HEIGHT);

            int cursorX = startX;
            for (AdventureText.Run run : runs) {
                font.drawString(run.display, cursorX, lineY, run.rgb);
                cursorX += font.getStringWidth(run.display);
            }
        }

        GL11.glEnable(GL11.GL_DEPTH_TEST);
        GL11.glDepthMask(true);
        GL11.glEnable(GL11.GL_LIGHTING);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    /** Semi-transparent background like the vanilla nametag (25% black). */
    private static void drawBackground(int x1, int y1, int x2, int y2) {
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(0.0F, 0.0F, 0.0F, 0.25F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
        GL11.glEnable(GL11.GL_TEXTURE_2D);
        GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
