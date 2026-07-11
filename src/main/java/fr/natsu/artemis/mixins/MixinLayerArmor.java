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
 * Masque des pièces d'armure d'un joueur (module Limb) : annule le rendu d'une pièce si elle est
 * cachée. {@code LayerArmorBase.renderLayer} est appelé par slot d'armure (1=bottes, 2=jambières,
 * 3=plastron, 4=casque), d'où {@code ArmorPiece = 5 - armorSlot}.
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
