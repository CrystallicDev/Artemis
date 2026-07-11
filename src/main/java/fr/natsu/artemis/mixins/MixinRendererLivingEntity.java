package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import fr.natsu.artemis.module.glow.GlowState;
import fr.natsu.artemis.render.GlowRenderer;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Injecte la couleur de surbrillance pendant la passe de rendu du FBO : remplace la couleur d'équipe
 * calculée par {@code setScoreTeamColor} par la couleur Apollo issue de {@link GlowState}.
 */
@Mixin(RendererLivingEntity.class)
public abstract class MixinRendererLivingEntity<T extends EntityLivingBase> {

    @ModifyVariable(
        method = "setScoreTeamColor",
        at = @At(value = "STORE", ordinal = 0),
        ordinal = 0)
    private int artemis$outlineColor(int original) {
        if (GlowRenderer.isRenderingOutline && GlowRenderer.renderTarget instanceof EntityPlayer) {
            return GlowState.color(GlowRenderer.renderTarget.getUniqueID());
        }
        return original;
    }
}
