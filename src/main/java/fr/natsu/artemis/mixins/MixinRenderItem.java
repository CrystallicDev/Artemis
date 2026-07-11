package fr.natsu.artemis.mixins;

import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.gen.Invoker;

import fr.natsu.artemis.module.glint.GlintState;
import fr.natsu.artemis.module.glint.GlintTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.RenderItem;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.client.resources.model.IBakedModel;
import net.minecraft.item.ItemStack;

/**
 * Glint d'enchantement coloré par item (convention {@code lunar.glint}), indépendant du chemin de
 * rendu (compatible OptiFine, qui court-circuite {@code renderEffect}).
 *
 * <p>On rend notre propre glint <b>après</b> le modèle, dans {@code renderItem(ItemStack, IBakedModel)},
 * en reproduisant la transform locale du modèle. L'item n'a pas besoin d'être réellement enchanté :
 * la seule NBT {@code lunar.glint} suffit à déclencher le glint coloré.</p>
 */
@Mixin(RenderItem.class)
public abstract class MixinRenderItem {

    @Invoker("renderModel")
    protected abstract void artemis$renderModel(IBakedModel model, int color);

    @Inject(
        method = "renderItem(Lnet/minecraft/item/ItemStack;Lnet/minecraft/client/resources/model/IBakedModel;)V",
        at = @At("RETURN"))
    private void artemis$coloredGlint(ItemStack stack, IBakedModel model, CallbackInfo ci) {
        if (model == null || model.isBuiltInRenderer() || !GlintState.hasGlint(stack)) {
            return;
        }

        int argb = GlintState.glintArgb(stack);

        // Reproduit la transform locale appliquée au modèle par renderItem (scale 0.5, translate -0.5).
        GlStateManager.pushMatrix();
        GlStateManager.scale(0.5F, 0.5F, 0.5F);
        GlStateManager.translate(-0.5F, -0.5F, -0.5F);
        artemis$renderColoredGlint(model, argb);
        GlStateManager.popMatrix();
    }

    /** Réplique la structure du {@code renderEffect} vanilla, mais avec la texture alpha + couleur. */
    private void artemis$renderColoredGlint(IBakedModel model, int argb) {
        GlStateManager.depthMask(false);
        GlStateManager.depthFunc(514);
        GlStateManager.disableLighting();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
        Minecraft.getMinecraft().getTextureManager().bindTexture(GlintTexture.get());
        GlStateManager.matrixMode(GL11.GL_TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float offset1 = (float) (Minecraft.getSystemTime() % 3000L) / 3000.0F / 8.0F;
        GlStateManager.translate(offset1, 0.0F, 0.0F);
        GlStateManager.rotate(-50.0F, 0.0F, 0.0F, 1.0F);
        this.artemis$renderModel(model, argb);
        GlStateManager.popMatrix();

        GlStateManager.pushMatrix();
        GlStateManager.scale(8.0F, 8.0F, 8.0F);
        float offset2 = (float) (Minecraft.getSystemTime() % 4873L) / 4873.0F / 8.0F;
        GlStateManager.translate(-offset2, 0.0F, 0.0F);
        GlStateManager.rotate(10.0F, 0.0F, 0.0F, 1.0F);
        this.artemis$renderModel(model, argb);
        GlStateManager.popMatrix();

        GlStateManager.matrixMode(GL11.GL_MODELVIEW);
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.enableLighting();
        GlStateManager.depthFunc(515);
        GlStateManager.depthMask(true);
        Minecraft.getMinecraft().getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
    }
}
