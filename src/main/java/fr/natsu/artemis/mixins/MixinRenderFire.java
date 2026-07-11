package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.coloredfire.ColoredFireState;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Teinte les flammes d'un joueur selon sa couleur Colored Fire. Le feu vanilla est dessiné en
 * {@code POSITION_TEX} multiplié par {@code GlStateManager.color(1,1,1,1)} — on redirige cette
 * couleur si le joueur a une couleur custom.
 */
@Mixin(Render.class)
public class MixinRenderFire {

    private static int artemis$fireColor;

    @Inject(method = "renderEntityOnFire", at = @At("HEAD"), require = 0)
    private void artemis$captureFireColor(Entity entity, double x, double y, double z, float partialTicks,
                                          CallbackInfo ci) {
        artemis$fireColor = entity instanceof EntityPlayer
            ? ColoredFireState.colorOrZero(entity.getUniqueID())
            : 0;
    }

    @Redirect(
        method = "renderEntityOnFire",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/GlStateManager;color(FFFF)V"),
        require = 0)
    private void artemis$tintFire(float r, float g, float b, float a) {
        if (artemis$fireColor != 0) {
            GlStateManager.color(
                ((artemis$fireColor >> 16) & 0xFF) / 255.0F,
                ((artemis$fireColor >> 8) & 0xFF) / 255.0F,
                (artemis$fireColor & 0xFF) / 255.0F,
                a);
        } else {
            GlStateManager.color(r, g, b, a);
        }
    }
}
