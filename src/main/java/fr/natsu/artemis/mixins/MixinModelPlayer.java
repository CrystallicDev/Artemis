package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.limb.LimbState;
import net.minecraft.client.model.ModelPlayer;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Masque des parties du corps d'un joueur (module Limb) : avant le rendu du modèle, on désactive
 * {@code showModel} des parties cachées (base + couche « wear »), puis on restaure après.
 */
@Mixin(ModelPlayer.class)
public class MixinModelPlayer {

    /** {@code showModel} d'origine des 12 parties, sauvegardé le temps du rendu. */
    private static boolean[] artemis$saved;

    @Inject(method = "render", at = @At("HEAD"), require = 0)
    private void artemis$hideParts(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                   float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        artemis$saved = null;
        if (!(entity instanceof EntityPlayer)) {
            return;
        }
        int mask = LimbState.maskOrZero(entity.getUniqueID());
        if (mask == 0) {
            return;
        }

        ModelRenderer[] parts = parts();
        artemis$saved = new boolean[parts.length];
        for (int i = 0; i < parts.length; i++) {
            artemis$saved[i] = parts[i].showModel;
        }

        hide(parts, mask, LimbState.HEAD, 0, 1);
        hide(parts, mask, LimbState.TORSO, 2, 3);
        hide(parts, mask, LimbState.LEFT_ARM, 4, 5);
        hide(parts, mask, LimbState.RIGHT_ARM, 6, 7);
        hide(parts, mask, LimbState.LEFT_LEG, 8, 9);
        hide(parts, mask, LimbState.RIGHT_LEG, 10, 11);
    }

    @Inject(method = "render", at = @At("RETURN"), require = 0)
    private void artemis$restoreParts(Entity entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                                      float netHeadYaw, float headPitch, float scale, CallbackInfo ci) {
        if (artemis$saved == null) {
            return;
        }
        ModelRenderer[] parts = parts();
        for (int i = 0; i < parts.length && i < artemis$saved.length; i++) {
            parts[i].showModel = artemis$saved[i];
        }
        artemis$saved = null;
    }

    private static void hide(ModelRenderer[] parts, int mask, int bodyPart, int base, int wear) {
        if ((mask & LimbState.bit(bodyPart)) != 0) {
            parts[base].showModel = false;
            parts[wear].showModel = false;
        }
    }

    /** Les 12 parties : base + couche wear, dans l'ordre head/torso/bras G/D/jambes G/D. */
    private ModelRenderer[] parts() {
        ModelPlayer model = (ModelPlayer) (Object) this;
        return new ModelRenderer[] {
            model.bipedHead, model.bipedHeadwear,
            model.bipedBody, model.bipedBodyWear,
            model.bipedLeftArm, model.bipedLeftArmwear,
            model.bipedRightArm, model.bipedRightArmwear,
            model.bipedLeftLeg, model.bipedLeftLegwear,
            model.bipedRightLeg, model.bipedRightLegwear
        };
    }
}
