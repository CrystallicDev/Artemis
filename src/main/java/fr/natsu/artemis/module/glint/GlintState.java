package fr.natsu.artemis.module.glint;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Reads an item's glint color, following the Lunar Client / Apollo convention.
 *
 * <p>The server puts a {@code lunar.glint} NBT tag on the item, an <b>ARGB</b> color (e.g.
 * {@code -65536} = {@code 0xFFFF0000}, red). That tag travels with the item through the normal
 * Minecraft packets; the colored rendering is done by {@code MixinRenderItem}.</p>
 */
public final class GlintState {

    /** The Lunar NBT compound put on the item. */
    public static final String LUNAR_TAG = "lunar";

    /** Key of the glint color (ARGB int) inside the {@link #LUNAR_TAG} compound. */
    public static final String GLINT_KEY = "glint";

    private GlintState() {
    }

    /** True if the item carries a {@code lunar.glint} color. */
    public static boolean hasGlint(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(LUNAR_TAG) && tag.getCompoundTag(LUNAR_TAG).hasKey(GLINT_KEY);
    }

    /** The item's glint color as ARGB (call after {@link #hasGlint}). */
    public static int glintArgb(ItemStack stack) {
        return stack.getTagCompound().getCompoundTag(LUNAR_TAG).getInteger(GLINT_KEY);
    }
}
