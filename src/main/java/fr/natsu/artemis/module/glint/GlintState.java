package fr.natsu.artemis.module.glint;

import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;

/**
 * Lecture de la couleur de glint sur un item, selon la convention Lunar Client / Apollo.
 *
 * <p>Le serveur pose sur l'item une NBT {@code lunar.glint} = couleur <b>ARGB</b> (ex. {@code -65536}
 * = {@code 0xFFFF0000}, rouge). Cette NBT voyage avec l'item par les packets Minecraft normaux ; le
 * rendu coloré est appliqué par {@code MixinRenderItem}.</p>
 */
public final class GlintState {

    /** Compound NBT Lunar posé sur l'item. */
    public static final String LUNAR_TAG = "lunar";

    /** Clé de la couleur de glint (int ARGB) dans le compound {@link #LUNAR_TAG}. */
    public static final String GLINT_KEY = "glint";

    private GlintState() {
    }

    /** Vrai si l'item porte une couleur de glint {@code lunar.glint}. */
    public static boolean hasGlint(ItemStack stack) {
        if (stack == null || !stack.hasTagCompound()) {
            return false;
        }
        NBTTagCompound tag = stack.getTagCompound();
        return tag.hasKey(LUNAR_TAG) && tag.getCompoundTag(LUNAR_TAG).hasKey(GLINT_KEY);
    }

    /** Couleur ARGB du glint de l'item (appeler après {@link #hasGlint}). */
    public static int glintArgb(ItemStack stack) {
        return stack.getTagCompound().getCompoundTag(LUNAR_TAG).getInteger(GLINT_KEY);
    }
}
