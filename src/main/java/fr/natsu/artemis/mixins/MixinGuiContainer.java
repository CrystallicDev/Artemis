package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.inventory.InventoryInteraction;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.WorldRenderer;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.inventory.Slot;

/**
 * Module Inventory (interaction) : dans un GUI de conteneur, gère les items tagués {@code lunar.*} —
 * annule le clic (action déclenchée / non-cliquable) et cache le surlignage de slot demandé.
 */
@Mixin(GuiContainer.class)
public abstract class MixinGuiContainer {

    @Shadow
    private Slot theSlot;

    @Inject(method = "handleMouseClick", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$inventoryClick(Slot slotIn, int slotId, int clickedButton, int clickType, CallbackInfo ci) {
        if (slotIn != null && slotIn.getHasStack() && InventoryInteraction.handleClick(slotIn.getStack())) {
            ci.cancel();
        }
    }

    @Redirect(
        method = "drawScreen",
        at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/inventory/GuiContainer;drawGradientRect(IIIIII)V"),
        require = 0)
    private void artemis$slotHighlight(GuiContainer instance, int left, int top, int right, int bottom,
                                       int startColor, int endColor) {
        if (this.theSlot != null && this.theSlot.getHasStack()
            && InventoryInteraction.hideHighlight(this.theSlot.getStack())) {
            return;
        }
        drawGradient(left, top, right, bottom, startColor, endColor);
    }

    /** Copie du {@code Gui.drawGradientRect} vanilla (le highlight de slot), z = 0. */
    private static void drawGradient(int left, int top, int right, int bottom, int startColor, int endColor) {
        float sa = (startColor >> 24 & 255) / 255.0F;
        float sr = (startColor >> 16 & 255) / 255.0F;
        float sg = (startColor >> 8 & 255) / 255.0F;
        float sb = (startColor & 255) / 255.0F;
        float ea = (endColor >> 24 & 255) / 255.0F;
        float er = (endColor >> 16 & 255) / 255.0F;
        float eg = (endColor >> 8 & 255) / 255.0F;
        float eb = (endColor & 255) / 255.0F;

        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.disableAlpha();
        GlStateManager.tryBlendFuncSeparate(770, 771, 1, 0);
        GlStateManager.shadeModel(7425);

        Tessellator tessellator = Tessellator.getInstance();
        WorldRenderer worldRenderer = tessellator.getWorldRenderer();
        worldRenderer.begin(7, DefaultVertexFormats.POSITION_COLOR);
        worldRenderer.pos(right, top, 0.0D).color(sr, sg, sb, sa).endVertex();
        worldRenderer.pos(left, top, 0.0D).color(sr, sg, sb, sa).endVertex();
        worldRenderer.pos(left, bottom, 0.0D).color(er, eg, eb, ea).endVertex();
        worldRenderer.pos(right, bottom, 0.0D).color(er, eg, eb, ea).endVertex();
        tessellator.draw();

        GlStateManager.shadeModel(7424);
        GlStateManager.disableBlend();
        GlStateManager.enableAlpha();
        GlStateManager.enableTexture2D();
    }
}
