package fr.natsu.artemis.module.glint;

import java.awt.image.BufferedImage;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureUtil;
import net.minecraft.util.ResourceLocation;

/**
 * Génère une version « alpha » de la texture de glint vanilla, façon Lunar : chaque pixel devient
 * blanc (RGB = 0xFFFFFF) avec l'alpha égal au niveau de gris d'origine.
 *
 * <p>Rendue ensuite teintée par une couleur de vertex, cette texture permet d'obtenir un glint de
 * <b>n'importe quelle couleur</b> — indépendamment du chemin de rendu vanilla/OptiFine.</p>
 */
public final class GlintTexture {

    private static final ResourceLocation VANILLA_GLINT =
        new ResourceLocation("textures/misc/enchanted_item_glint.png");

    private static ResourceLocation location;

    private GlintTexture() {
    }

    /** Emplacement de la texture alpha (générée à la première demande, avec un contexte GL valide). */
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
            Artemis.LOGGER.info("[Glint] texture alpha generee ({}x{})", width, height);
        } catch (Exception e) {
            Artemis.LOGGER.error("[Glint] echec de generation de la texture alpha, fallback vanilla", e);
            location = VANILLA_GLINT;
        }
    }
}
