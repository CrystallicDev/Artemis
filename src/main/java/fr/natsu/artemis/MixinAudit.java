package fr.natsu.artemis;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public final class MixinAudit {

    private static final String[] TARGETS = {
        "net.minecraft.network.NetworkManager",
        "net.minecraft.entity.Entity",
        "net.minecraft.client.renderer.RenderGlobal",
        "net.minecraft.client.renderer.entity.RendererLivingEntity",
        "net.minecraft.client.renderer.entity.RenderItem",
        "net.minecraft.client.renderer.entity.Render",
        "net.minecraft.client.model.ModelPlayer",
        "net.minecraft.client.renderer.entity.layers.LayerArmorBase",
        "net.minecraft.client.gui.inventory.GuiContainer",
        "net.minecraft.client.gui.GuiScreen",
        "net.minecraft.client.gui.FontRenderer",
        "net.minecraft.network.play.server.S3EPacketTeams"
    };

    private MixinAudit() {
    }

    public static void report() {
        for (String name : TARGETS) {
            try {
                Class<?> type = Class.forName(name, false, MixinAudit.class.getClassLoader());
                List<String> handlers = new ArrayList<String>();
                for (Method method : type.getDeclaredMethods()) {
                    int index = method.getName().indexOf("artemis$");
                    if (index >= 0) {
                        handlers.add(method.getName().substring(index + 8));
                    }
                }
                Artemis.LOGGER.info("[Mixin] {} -> {}", type.getSimpleName(),
                    handlers.isEmpty() ? "NO HANDLER APPLIED" : handlers);
            } catch (Throwable t) {
                Artemis.LOGGER.warn("[Mixin] {} -> not loadable: {}", name, t.toString());
            }
        }
    }
}
