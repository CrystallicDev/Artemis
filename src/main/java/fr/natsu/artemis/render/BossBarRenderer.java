package fr.natsu.artemis.render;

import fr.natsu.artemis.module.model.ModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.boss.BossStatus;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class BossBarRenderer {

    private static final int BAR_WIDTH = 182;
    private static final int BAR_HEIGHT = 5;
    private static final int TEXTURE_HEIGHT = 10;
    private static final int NOTCH_COLOR = 0xFF000000;

    @SubscribeEvent
    public void onRenderOverlay(RenderGameOverlayEvent.Pre event) {
        if (event.type != RenderGameOverlayEvent.ElementType.BOSSHEALTH || !ModelRegistry.hasBossBars()) {
            return;
        }
        if (BossStatus.bossName == null || BossStatus.statusBarTime <= 0) {
            return;
        }
        ModelRegistry.BossBarEntry entry = ModelRegistry.findForBossBar(BossStatus.bossName);
        if (entry == null) {
            return;
        }

        event.setCanceled(true);
        BossStatus.statusBarTime--;
        render(entry);
    }

    private static void render(ModelRegistry.BossBarEntry entry) {
        Minecraft mc = Minecraft.getMinecraft();
        ScaledResolution resolution = new ScaledResolution(mc);
        int centerX = resolution.getScaledWidth() / 2;
        int x = centerX - BAR_WIDTH / 2;
        int y = entry.y;

        int filled = (int) (BossStatus.healthScale * (float) (BAR_WIDTH + 1));
        if (filled > BAR_WIDTH) {
            filled = BAR_WIDTH;
        }
        if (filled < 0) {
            filled = 0;
        }

        GlStateManager.enableBlend();
        if (entry.texturePath != null) {
            mc.getTextureManager().bindTexture(ModelRegistry.resource(entry.texturePath));
            setColor(entry.color);
            Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, 0.0F, BAR_WIDTH, BAR_HEIGHT,
                BAR_WIDTH, BAR_HEIGHT, BAR_WIDTH, TEXTURE_HEIGHT);
            if (filled > 0) {
                Gui.drawScaledCustomSizeModalRect(x, y, 0.0F, BAR_HEIGHT, filled, BAR_HEIGHT,
                    filled, BAR_HEIGHT, BAR_WIDTH, TEXTURE_HEIGHT);
            }
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        } else {
            Gui.drawRect(x, y, x + BAR_WIDTH, y + BAR_HEIGHT, entry.backgroundColor);
            if (filled > 0) {
                Gui.drawRect(x, y, x + filled, y + BAR_HEIGHT, entry.color);
            }
        }

        if (entry.notches > 1) {
            for (int i = 1; i < entry.notches; i++) {
                int notchX = x + BAR_WIDTH * i / entry.notches;
                Gui.drawRect(notchX, y, notchX + 1, y + BAR_HEIGHT, NOTCH_COLOR);
            }
        }

        if (!entry.hideName) {
            FontRenderer font = mc.fontRendererObj;
            String name = BossStatus.bossName;
            font.drawStringWithShadow(name, centerX - font.getStringWidth(name) / 2.0F,
                y - 10.0F, entry.textColor);
        }

        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private static void setColor(int argb) {
        GlStateManager.color(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
            (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
    }
}
