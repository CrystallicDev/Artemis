package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.module.glow.GlowState;
import fr.natsu.artemis.render.GlowRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
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

    @Inject(method = "setScoreTeamColor", at = @At("HEAD"), cancellable = true)
    private void artemis$glowColor(EntityLivingBase entity, CallbackInfoReturnable<Boolean> cir) {
        if (!GlowRenderer.isRenderingOutline || !(entity instanceof EntityPlayer)
            || !GlowState.isGlowing(entity.getUniqueID())) {
            return;
        }

        int rgb = GlowState.color(entity.getUniqueID());
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
