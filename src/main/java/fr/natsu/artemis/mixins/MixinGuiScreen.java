package fr.natsu.artemis.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import fr.natsu.artemis.module.inventory.InventoryInteraction;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.ItemStack;

/**
 * Inventory (interaction) module: hides the tooltip of items tagged {@code lunar.hideItemTooltip}.
 */
@Mixin(GuiScreen.class)
public class MixinGuiScreen {

    @Inject(method = "renderToolTip", at = @At("HEAD"), cancellable = true, require = 0)
    private void artemis$hideTooltip(ItemStack stack, int x, int y, CallbackInfo ci) {
        if (InventoryInteraction.hideTooltip(stack)) {
            ci.cancel();
        }
    }
}
