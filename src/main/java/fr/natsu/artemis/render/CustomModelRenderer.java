package fr.natsu.artemis.render;

import fr.natsu.artemis.module.model.JemAnimation;
import fr.natsu.artemis.module.model.JemModel;
import fr.natsu.artemis.module.model.ModelAnimator;
import fr.natsu.artemis.module.model.ModelRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.culling.ICamera;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.util.AxisAlignedBB;
import net.minecraftforge.client.MinecraftForgeClient;

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
    private static final Map<Entity, Float> RENDER_RANGE = new WeakHashMap<Entity, Float>();

    private static final Set<Entity> RENDERED_FRAME = Collections.newSetFromMap(new WeakHashMap<Entity, Boolean>());

    public static void beginFrame() {
        if (MinecraftForgeClient.getRenderPass() == 0) {
            RENDERED_FRAME.clear();
        }
    }

    public static void renderMissed(Entity viewer, ICamera camera, float partialTicks) {
        if (RENDER_RANGE.isEmpty() || MinecraftForgeClient.getRenderPass() != 0) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        RenderManager renderManager = mc.getRenderManager();
        double camX = renderManager.viewerPosX;
        double camY = renderManager.viewerPosY;
        double camZ = renderManager.viewerPosZ;
        Frustum own = null;

        for (Map.Entry<Entity, Float> entry : RENDER_RANGE.entrySet()) {
            Entity entity = entry.getKey();
            if (entity == viewer || entity.isDead || RENDERED_FRAME.contains(entity)) {
                continue;
            }
            double dx = entity.posX - camX;
            double dy = entity.posY - camY;
            double dz = entity.posZ - camZ;
            float range = entry.getValue();
            if (dx * dx + dy * dy + dz * dz >= (double) range * (double) range) {
                continue;
            }
            AxisAlignedBB box = entity.getEntityBoundingBox();
            boolean cameraFrustum = camera != null && camera.isBoundingBoxInFrustum(box);
            boolean ownFrustum = cameraFrustum;
            if (!cameraFrustum && !entity.ignoreFrustumCheck) {
                if (own == null) {
                    own = new Frustum();
                    own.setPosition(camX, camY, camZ);
                }
                ownFrustum = own.isBoundingBoxInFrustum(box);
            }
            if (!entity.ignoreFrustumCheck && !ownFrustum) {
                continue;
            }
            renderManager.renderEntitySimple(entity, partialTicks);
        }
    }
    public static float renderRange(Entity entity) {
        Float range = RENDER_RANGE.get(entity);
        return range == null ? 0.0F : range;
    }

    @SubscribeEvent
    public void onRenderTick(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            partialTicks = event.renderTickTime;
        }
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        ModelRegistry.ensureLoaded();
        if (ModelRegistry.isEmpty()) {
            return;
        }
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.theWorld == null) {
            return;
        }
        RENDER_RANGE.clear();
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
                RENDER_RANGE.put(entity, distance);
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
        RENDERED_FRAME.add(entity);

        model.resetAnimation();
        JemAnimation animation = ModelRegistry.getAnimation(entry);
        if (animation != null) {
            ModelAnimator.apply(model, animation, (entity.ticksExisted + partialTicks) / 20.0D);
        }
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

        model.render();

        GlStateManager.disableRescaleNormal();
        GlStateManager.color(1.0F, 1.0F, 1.0F, 1.0F);
        GlStateManager.popMatrix();
    }
}
