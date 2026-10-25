package fr.natsu.artemis.mixins;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Dynamic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.module.glow.GlowState;
import fr.natsu.artemis.render.GlowRenderer;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.scoreboard.ScorePlayerTeam;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @Inject(method = "renderLayers", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$skipLayersInOutline(T entity, float limbSwing, float limbSwingAmount,
            float partialTicks, float ageInTicks, float netHeadYaw, float headPitch, float scale,
            CallbackInfo ci) {
        if (GlowRenderer.isRenderingOutline) {
            ci.cancel();
        }
    }

    @Dynamic
    @Inject(method = "lunar$setScoreTeamColor", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void artemis$lunarOutlineColor(EntityLivingBase entity, CallbackInfoReturnable<Integer> cir) {
        int rgb = 0xFFFFFF;
        if (GlowRenderer.isRenderingOutline && GlowState.isGlowing(entity.getUniqueID())) {
            rgb = GlowState.color(entity.getUniqueID()) & 0xFFFFFF;
        } else if (entity instanceof EntityPlayer && entity.getTeam() instanceof ScorePlayerTeam) {
            rgb = teamRgb(FontRenderer.getFormatFromString(((ScorePlayerTeam) entity.getTeam()).getColorPrefix()));
        }
        GlStateManager.resetColor();
        GL11.glDisable(GL11.GL_LIGHTING);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GL11.glColor4f((rgb >> 16 & 0xFF) / 255.0F, (rgb >> 8 & 0xFF) / 255.0F, (rgb & 0xFF) / 255.0F, 1.0F);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GL11.glDisable(GL11.GL_TEXTURE_2D);
        OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
        cir.setReturnValue(rgb);
    }

    private static int teamRgb(String format) {
        if (format.length() >= 14 && Character.toLowerCase(format.charAt(1)) == 'x') {
            int rgb = 0;
            for (int i = 0; i < 6; i++) {
                int digit = Character.digit(format.charAt(3 + i * 2), 16);
                if (digit < 0) {
                    return 0xFFFFFF;
                }
                rgb = rgb << 4 | digit;
            }
            return rgb;
        }
        if (format.length() >= 2) {
            int index = "0123456789abcdef".indexOf(Character.toLowerCase(format.charAt(1)));
            if (index >= 0) {
                return Minecraft.getMinecraft().fontRendererObj.getColorCode(format.charAt(1)) & 0xFFFFFF;
            }
        }
        return 0xFFFFFF;
    }

    @Inject(method = "setScoreTeamColor", at = @At("HEAD"), cancellable = true)
    private void artemis$glowColor(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        int rgb;
        if (GlowRenderer.isRenderingOutline && GlowState.isGlowing(entity.getUniqueID())) {
            rgb = GlowState.color(entity.getUniqueID());
        } else {
            String format = entity.getTeam() instanceof ScorePlayerTeam
                ? FontRenderer.getFormatFromString(((ScorePlayerTeam) entity.getTeam()).getColorPrefix()) : "";
            if (format.length() < 2 || Character.toLowerCase(format.charAt(1)) != 'x') {
                return;
            }
            rgb = teamRgb(format);
        }

        float red = (rgb >> 16 & 0xFF) / 255.0F;
        float green = (rgb >> 8 & 0xFF) / 255.0F;
        float blue = (rgb & 0xFF) / 255.0F;

        GlStateManager.disableLighting();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        GlStateManager.color(red, green, blue, 1.0F);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.lightmapTexUnit);
        GlStateManager.disableTexture2D();
        GlStateManager.setActiveTexture(OpenGlHelper.defaultTexUnit);
        cir.setReturnValue(true);
    }
}
