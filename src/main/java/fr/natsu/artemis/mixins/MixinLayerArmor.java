package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.limb.LimbState;
import fr.natsu.artemis.module.model.ModelRegistry;
import fr.natsu.artemis.render.JemItemRenderer;
import net.minecraft.item.ItemStack;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

@Mixin(LayerArmorBase.class)
public class MixinLayerArmor {

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$hideArmor(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                   float partialTicks, float ageInTicks, float netHeadYaw, float headPitch,
                                   float scale, int armorSlot, CallbackInfo ci) {
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        int piece = 5 - armorSlot;
        if ((LimbState.armorMaskOrZero(entity.getUniqueID()) & LimbState.bit(piece)) != 0) {
            ci.cancel();
        }
    }

    @Inject(method = "renderLayer", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$customArmorModel(EntityLivingBase entity, float limbSwing, float limbSwingAmount,
                                          float partialTicks, float ageInTicks, float netHeadYaw,
                                          float headPitch, float scale, int armorSlot, CallbackInfo ci) {
        if (!ModelRegistry.hasItems()) {
            return;
        }
        ItemStack stack = entity.getEquipmentInSlot(armorSlot);
        if (stack != null && JemItemRenderer.renderArmor(entity, stack, armorSlot)) {
            ci.cancel();
        }
    }
}
