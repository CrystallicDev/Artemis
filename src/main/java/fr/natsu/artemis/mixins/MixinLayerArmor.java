package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.limb.LimbState;
import net.minecraft.client.renderer.entity.layers.LayerArmorBase;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Hides a player's armor pieces (Limb module): skips rendering a piece when it's hidden.
 * {@code LayerArmorBase.renderLayer} is called per armor slot (1=boots, 2=leggings, 3=chestplate,
 * 4=helmet), hence {@code ArmorPiece = 5 - armorSlot}.
 */
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
}
