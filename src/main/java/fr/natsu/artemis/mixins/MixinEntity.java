package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import fr.natsu.artemis.render.CustomModelRenderer;
import net.minecraft.entity.Entity;

@Mixin(Entity.class)
public abstract class MixinEntity {

    @Inject(method = "isInRangeToRender3d", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$customRenderRange(double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        Entity self = (Entity) (Object) this;
        float range = CustomModelRenderer.renderRange(self);
        if (range <= 0.0F) {
            return;
        }
        double dx = self.posX - x;
        double dy = self.posY - y;
        double dz = self.posZ - z;
        cir.setReturnValue(dx * dx + dy * dy + dz * dz < (double) range * (double) range);
    }
}
