package fr.natsu.artemis.render;

import fr.natsu.artemis.module.model.JemModel;
import fr.natsu.artemis.module.model.ModelRegistry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraftforge.client.event.RenderLivingEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

public final class CustomModelRenderer {
    private static final float MODEL_OFFSET_Y = -1.5078125F;
    private static final float DEFAULT_RENDER_DISTANCE = 256.0F;

    private static float partialTicks;

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            partialTicks = event.renderTickTime;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END || ModelRegistry.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        for (Entity entity : mc.theWorld.loadedEntityList) {
            ModelRegistry.Entry entry = ModelRegistry.findForEntity(entity);
            if (entry == null) {
                continue;
            }
            if (entry.ignoreCull) {
                entity.ignoreFrustumCheck = true;
            }
            float distance = entry.renderDistance > 0.0F ? entry.renderDistance
                : (entry.ignoreCull ? DEFAULT_RENDER_DISTANCE : 0.0F);
            if (distance > 0.0F) {
                entity.renderDistanceWeight = renderDistanceWeight(entity, distance);
            }
        }
    }

    private static double renderDistanceWeight(Entity entity, float blocks) {
        double edge = entity.getEntityBoundingBox().getAverageEdgeLength();
        if (Double.isNaN(edge) || edge <= 0.0D) {
            edge = 1.0D;
        }
        return blocks / (edge * 64.0D);
    }

    @SubscribeEvent
    public void onRenderLiving(RenderLivingEvent.Pre<EntityLivingBase> event) {
        if (ModelRegistry.isEmpty()) {
            return;
        }
        EntityLivingBase entity = event.entity;
        ModelRegistry.Entry entry = ModelRegistry.findForEntity(entity);
        if (entry == null) {
            return;
        }
        JemModel model = ModelRegistry.getModel(entry);
        if (model == null) {
            return;
        }

        event.setCanceled(true);
        render(entity, model, entry.scale, event.x, event.y, event.z);
    }

    private static void render(EntityLivingBase entity, JemModel model, float scale,
                               double x, double y, double z) {
        float yaw = entity.prevRenderYawOffset
            + (entity.renderYawOffset - entity.prevRenderYawOffset) * partialTicks;

        GlStateManager.pushMatrix();
        GlStateManager.translate(x, y, z);
        GlStateManager.rotate(180.0F - yaw, 0.0F, 1.0F, 0.0F);
        GlStateManager.enableRescaleNormal();
        GlStateManager.scale(-1.0F, -1.0F, 1.0F);
        if (scale != 1.0F) {
            GlStateManager.scale(scale, scale, scale);
        }
        GlStateManager.translate(0.0F, MODEL_OFFSET_Y, 0.0F);

        model.resetAnimation();
        model.render();

        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
