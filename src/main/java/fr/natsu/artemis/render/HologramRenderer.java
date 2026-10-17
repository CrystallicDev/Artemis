package fr.natsu.artemis.render;

import java.util.List;

import org.lwjgl.opengl.GL11;

import fr.natsu.artemis.module.hologram.HologramState;
import fr.natsu.artemis.module.nametag.AdventureText.Run;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class HologramRenderer {

    private static final float TEXT_SCALE = 0.02666667F;
    private static final double MAX_RENDER_DIST_SQ = 16384.0D;

    @SubscribeEvent
    public void onRenderWorld(RenderWorldLastEvent event) {
        if (HologramState.isEmpty()) {
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null || mc.getRenderViewEntity() == null) {
            return;
        }

        RenderManager renderManager = mc.getRenderManager();

        Vec3 camera = new Vec3(renderManager.viewerPosX, renderManager.viewerPosY, renderManager.viewerPosZ);
        FontRenderer font = mc.fontRendererObj;

        for (HologramState.Hologram hologram : HologramState.all()) {
            double dx = hologram.x - camera.xCoord;
            double dy = hologram.y - camera.yCoord;
            double dz = hologram.z - camera.zCoord;
            if (dx * dx + dy * dy + dz * dz > MAX_RENDER_DIST_SQ) {
                continue;
            }
            render(mc, font, hologram, dx, dy, dz);
        }
    }

    private void render(Minecraft mc, FontRenderer font, HologramState.Hologram hologram,
                        double dx, double dy, double dz) {
        int lineHeight = font.FONT_HEIGHT + 1;
        int count = hologram.lines.size();

        GL11.glPushMatrix();
        GL11.glTranslated(dx, dy, dz);
        GL11.glNormal3f(0.0F, 1.0F, 0.0F);
        GL11.glRotatef(-mc.getRenderManager().playerViewY, 0.0F, 1.0F, 0.0F);
        GL11.glRotatef(mc.getRenderManager().playerViewX, 1.0F, 0.0F, 0.0F);
        GL11.glScalef(-TEXT_SCALE, -TEXT_SCALE, TEXT_SCALE);

        GL11.glDisable(GL11.GL_LIGHTING);
        GL11.glDepthMask(false);
        GL11.glEnable(GL11.GL_BLEND);
        GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        if (hologram.showThroughWalls) {
            GL11.glDisable(GL11.GL_DEPTH_TEST);
        }

        int blockHeight = count * lineHeight;
        for (int i = 0; i < count; i++) {
            List<Run> runs = hologram.lines.get(i);
            int width = 0;
            for (Run run : runs) {
                width += font.getStringWidth(run.display);
            }
            int lineY = i * lineHeight - blockHeight / 2;
            int startX = -width / 2;

            if (hologram.showBackground) {
                drawBackground(startX - 1, lineY - 1, startX + width + 1, lineY + font.FONT_HEIGHT);
            }

            int cursorX = startX;
            for (Run run : runs) {
                if (hologram.showShadow) {
                    font.drawStringWithShadow(run.display, cursorX, lineY, 0xFF000000 | run.rgb);
                } else {
                    font.drawString(run.display, cursorX, lineY, run.rgb);
                }
                cursorX += font.getStringWidth(run.display);
            }
        }

        if (hologram.showThroughWalls) {
            GL11.glEnable(GL11.GL_DEPTH_TEST);
        }
        GL11.glDepthMask(true);
        GL11.glDisable(GL11.GL_BLEND);
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GL11.glPopMatrix();
    }

    private static void drawBackground(int x1, int y1, int x2, int y2) {
        GlStateManager.disableTexture2D();
        GlStateManager.color(0.0F, 0.0F, 0.0F, 0.25F);
        GL11.glBegin(GL11.GL_QUADS);
        GL11.glVertex2f(x1, y2);
        GL11.glVertex2f(x2, y2);
        GL11.glVertex2f(x2, y1);
        GL11.glVertex2f(x1, y1);
        GL11.glEnd();
        GlStateManager.enableTexture2D();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }
}
