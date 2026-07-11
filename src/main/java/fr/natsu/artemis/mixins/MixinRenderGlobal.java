package fr.natsu.artemis.mixins;

import java.util.List;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import fr.natsu.artemis.render.GlowRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.entity.Entity;

/**
 * Branche la passe 1 du Glowing dans {@code RenderGlobal.renderEntities}, au moment où la caméra et
 * les positions interpolées sont disponibles. Point d'injection identique à celui d'Eterion.
 */
@Mixin(RenderGlobal.class)
public abstract class MixinRenderGlobal {

    @Inject(
        method = "renderEntities",
        require = 0,
        at = @At(
            value = "INVOKE_STRING",
            target = "Lnet/minecraft/profiler/Profiler;endStartSection(Ljava/lang/String;)V",
            shift = At.Shift.BEFORE,
            ordinal = 2,
            args = "ldc=entities"),
        locals = LocalCapture.CAPTURE_FAILSOFT)
    private void artemis$renderGlow(Entity renderViewEntity, ICamera camera, float partialTicks,
                                    CallbackInfo ci, int pass,
                                    double d0, double d1, double d2, Entity entity,
                                    double d3, double d4, double d5, List<Entity> list,
                                    boolean bool0, boolean bool1) {
        GlowRenderer.renderEntitiesToFramebuffer(renderViewEntity, camera, partialTicks);
    }
}
