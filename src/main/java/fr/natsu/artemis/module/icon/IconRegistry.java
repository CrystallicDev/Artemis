package fr.natsu.artemis.module.icon;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import fr.natsu.artemis.Artemis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.IReloadableResourceManager;
import net.minecraft.client.resources.IResource;
import net.minecraft.client.resources.IResourceManager;
import net.minecraft.client.resources.IResourceManagerReloadListener;
import net.minecraft.util.ResourceLocation;

public final class IconRegistry {

    private static final String BASE_PATH = "artemis/";
    private static final ResourceLocation INDEX = new ResourceLocation("minecraft", BASE_PATH + "icons.json");

    private static final Map<String, Icon> ICONS = new HashMap<>();

    public static final class Icon {
        public final ResourceLocation texture;
        public final float width;
        public final float height;
        public final float ascent;

        Icon(ResourceLocation texture, float width, float height, float ascent) {
            this.texture = texture;
            this.width = width;
            this.height = height;
            this.ascent = ascent;
        }
    }

    private IconRegistry() {
    }

    public static void init() {
        IResourceManager resourceManager = Minecraft.getMinecraft().getResourceManager();
        if (resourceManager instanceof IReloadableResourceManager) {
            ((IReloadableResourceManager) resourceManager).registerReloadListener(
                new IResourceManagerReloadListener() {
                    @Override
                    public void onResourceManagerReload(IResourceManager manager) {
                        reload(manager);
                    }
                });
        } else {
            reload(resourceManager);
        }
    }

    public static void reload(IResourceManager resourceManager) {
        ICONS.clear();

        JsonObject root;
        try {
            IResource resource = resourceManager.getResource(INDEX);
            root = new JsonParser().parse(new InputStreamReader(resource.getInputStream(), "UTF-8"))
                .getAsJsonObject();
        } catch (java.io.IOException e) {
            return;
        } catch (Exception e) {
            Artemis.LOGGER.error("[Icon] could not parse '{}'", INDEX, e);
            return;
        }

        JsonArray icons = root.getAsJsonArray("icons");
        if (icons != null) {
            for (JsonElement element : icons) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject object = element.getAsJsonObject();
                JsonElement name = object.get("name");
                JsonElement texture = object.get("texture");
                if (name == null || texture == null) {
                    continue;
                }
                ICONS.put(name.getAsString(), new Icon(
                    resolve(texture.getAsString()),
                    number(object, "width", 8.0F),
                    number(object, "height", 8.0F),
                    number(object, "ascent", 0.0F)));
            }
        }
        Artemis.LOGGER.info("[Icon] loaded {} chat icon(s)", ICONS.size());
    }

    public static boolean isEmpty() {
        return ICONS.isEmpty();
    }

    public static Icon get(String name) {
        return ICONS.get(name);
    }

    private static ResourceLocation resolve(String value) {
        String path = value.endsWith(".png") ? value : value + ".png";
        if (path.indexOf(':') >= 0) {
            return new ResourceLocation(path);
        }
        return new ResourceLocation("minecraft", BASE_PATH + path);
    }

    private static float number(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsFloat();
    }
}
