package fr.natsu.artemis.module.glint;

import java.awt.image.BufferedImage;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

/**
 * Builds an "alpha" version of the vanilla glint texture, Lunar-style: every pixel becomes white
 * (RGB = 0xFFFFFF) with its alpha set to the original grey level.
 *
 * <p>Drawn tinted by a vertex color, this texture lets us get a glint of <b>any color</b>, independent
 * of the vanilla/OptiFine glint path.</p>
 */
public final class GlintTexture {

    private static final ResourceLocation VANILLA_GLINT =
        new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private static ResourceLocation location;

    private GlintTexture() {
    }

    /** The alpha texture location (built on first use, with a valid GL context). */
    public static ResourceLocation get() {
        if (location == null) {
            build();
        }
        return location;
    }

    private static void build() {
        Minecraft mc = Minecraft.getMinecraft();
        try {
            BufferedImage src = TextureUtil.readBufferedImage(
                mc.getResourceManager().getResource(VANILLA_GLINT).getInputStream());

            int width = src.getWidth();
            int height = src.getHeight();
            BufferedImage out = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int p = src.getRGB(x, y);
                    int gray = (((p >> 16) & 0xFF) + ((p >> 8) & 0xFF) + (p & 0xFF)) / 3 & 0xFF;
                    out.setRGB(x, y, (gray << 24) | 0xFFFFFF);
                }
            }

            location = mc.getTextureManager().getDynamicTextureLocation("artemis_glint", new DynamicTexture(out));
            Artemis.LOGGER.info("[Glint] alpha texture generated ({}x{})", width, height);
        } catch (Exception e) {
            Artemis.LOGGER.error("[Glint] failed to generate the alpha texture, falling back to vanilla", e);
            location = VANILLA_GLINT;
        }
    }
}
