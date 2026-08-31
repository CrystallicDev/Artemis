package fr.natsu.artemis.render;

import java.util.ArrayList;
import java.util.List;

import fr.natsu.artemis.module.model.JemAnimation;
import fr.natsu.artemis.module.model.JemModel;
import fr.natsu.artemis.module.model.ModelAnimator;
import fr.natsu.artemis.module.model.ModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelBiped;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RendererLivingEntity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.item.ItemStack;

public final class JemItemRenderer {

    private static final float PIXEL = 0.0625F;
    private static final float ITEM_SCALE = 0.5F;

    private JemItemRenderer() {
    }

    public static boolean renderHeld(ItemStack stack) {
        ModelRegistry.ItemEntry entry = ModelRegistry.findForItem(stack);
        if (entry == null) {
            return false;
        }
        JemModel model = prepare(entry);
        if (model == null) {
            return false;
        }

        GlStateManager.pushMatrix();
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE);
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        applyEntryTransform(entry);
        model.render();
        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
        return true;
    }

    public static boolean renderArmor(EntityLivingBase entity, ItemStack stack, int armorSlot) {
        ModelRegistry.ItemEntry entry = ModelRegistry.findForItem(stack);
        if (entry == null) {
            return false;
        }
        ModelBiped biped = resolveBiped(entity);
        if (biped == null) {
            return false;
        }
        List<ModelRenderer> parts = resolveParts(biped, entry.attach, armorSlot);
        if (parts.isEmpty()) {
            return false;
        }
        JemModel model = prepare(entry);
        if (model == null) {
            return false;
        }

        for (ModelRenderer part : parts) {
            GlStateManager.pushMatrix();
            part.postRender(PIXEL);
            applyEntryTransform(entry);
            model.render();
            GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
            GlStateManager.popMatrix();
        }
        return true;
    }

    private static JemModel prepare(ModelRegistry.ItemEntry entry) {
        JemModel model = ModelRegistry.getModel(entry.modelPath);
        if (model == null) {
            return null;
        }
        model.resetAnimation();
        JemAnimation animation = ModelRegistry.getAnimation(entry.animationPath, entry.animationName);
        if (animation != null) {
            ModelAnimator.apply(model, animation, Minecraft.getSystemTime() / 1000.0D);
        }
        return model;
    }

    private static void applyEntryTransform(ModelRegistry.ItemEntry entry) {
        if (entry.offset[0] != 0.0F || entry.offset[1] != 0.0F || entry.offset[2] != 0.0F) {
            GlStateManager.translate(entry.offset[0] * PIXEL, entry.offset[1] * PIXEL, entry.offset[2] * PIXEL);
        }
        if (entry.scale != 1.0F) {
            GlStateManager.scale(entry.scale, entry.scale, entry.scale);
        }
    }

    private static ModelBiped resolveBiped(EntityLivingBase entity) {
        Render<?> render = Minecraft.getMinecraft().getRenderManager().getEntityRenderObject(entity);
        if (!(render instanceof RendererLivingEntity)) {
            return null;
        }
        ModelBase model = ((RendererLivingEntity<?>) render).getMainModel();
        return model instanceof ModelBiped ? (ModelBiped) model : null;
    }

    private static List<ModelRenderer> resolveParts(ModelBiped biped, String attach, int armorSlot) {
        List<ModelRenderer> parts = new ArrayList<>();
        String target = attach;
        if (target == null) {
            switch (armorSlot) {
                case 4:
                    target = "head";
                    break;
                case 3:
                case 2:
                    target = "body";
                    break;
                default:
                    target = "legs";
                    break;
            }
        }

        if (target.equals("head")) {
            parts.add(biped.bipedHead);
        } else if (target.equals("body")) {
            parts.add(biped.bipedBody);
        } else if (target.equals("left_arm")) {
            parts.add(biped.bipedLeftArm);
        } else if (target.equals("right_arm")) {
            parts.add(biped.bipedRightArm);
        } else if (target.equals("left_leg")) {
            parts.add(biped.bipedLeftLeg);
        } else if (target.equals("right_leg")) {
            parts.add(biped.bipedRightLeg);
        } else if (target.equals("legs")) {
            parts.add(biped.bipedLeftLeg);
            parts.add(biped.bipedRightLeg);
        } else if (target.equals("arms")) {
            parts.add(biped.bipedLeftArm);
            parts.add(biped.bipedRightArm);
        }
        return parts;
    }
}
