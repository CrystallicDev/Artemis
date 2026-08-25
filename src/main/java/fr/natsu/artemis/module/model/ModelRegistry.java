package fr.natsu.artemis.module.model;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityList;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public final class ModelRegistry {
    private static final String BASE_PATH = "artemis/cem/";
    private static final ResourceLocation INDEX = new ResourceLocation("minecraft", BASE_PATH + "index.json");

    private static final List<Entry> ENTITY_ENTRIES = new ArrayList<>();
    private static final Map<String, JemModel> MODELS = new HashMap<>();

    public static final class Entry {
        public final String modelPath;
        public final String entityId;
        public final NameMatcher name;
        public final float scale;
        public final boolean ignoreCull;
        public final float renderDistance;

        Entry(String modelPath, String entityId, NameMatcher name, float scale,
              boolean ignoreCull, float renderDistance) {
            this.modelPath = modelPath;
            this.entityId = entityId;
            this.name = name;
            this.scale = scale;
            this.ignoreCull = ignoreCull;
            this.renderDistance = renderDistance;
        }
    }

    private ModelRegistry() {
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
        for (JemModel model : MODELS.values()) {
            model.dispose();
        }
        MODELS.clear();
        ENTITY_ENTRIES.clear();

        JsonObject index;
        try {
            IResource resource = resourceManager.getResource(INDEX);
            index = new JsonParser().parse(new InputStreamReader(resource.getInputStream(), "UTF-8"))
                .getAsJsonObject();
        } catch (Exception e) {
            return;
        }

        JsonArray entities = index.getAsJsonArray("entities");
        if (entities != null) {
            for (JsonElement element : entities) {
                if (element.isJsonObject()) {
                    Entry entry = parseEntry(element.getAsJsonObject());
                    if (entry != null) {
                        ENTITY_ENTRIES.add(entry);
                    }
                }
            }
        }
        Artemis.LOGGER.info("[Model] loaded {} entity override(s)", ENTITY_ENTRIES.size());
    }

    private static Entry parseEntry(JsonObject object) {
        JsonElement model = object.get("model");
        if (model == null || !model.isJsonPrimitive()) {
            return null;
        }
        JsonElement entity = object.get("entity");
        JsonElement name = object.get("name");
        JsonElement scale = object.get("scale");
        JsonElement ignoreCull = object.get("ignoreCull");
        JsonElement renderDistance = object.get("renderDistance");
        return new Entry(model.getAsString(),
            entity == null ? null : entity.getAsString(),
            name == null ? null : NameMatcher.compile(name.getAsString()),
            scale == null ? 1.0F : scale.getAsFloat(),
            ignoreCull != null && ignoreCull.getAsBoolean(),
            renderDistance == null ? 0.0F : renderDistance.getAsFloat());
    }

    public static boolean isEmpty() {
        return ENTITY_ENTRIES.isEmpty();
    }

    public static Entry findForEntity(Entity entity) {
        if (ENTITY_ENTRIES.isEmpty()) {
            return null;
        }
        String type = EntityList.getEntityString(entity);
        String customName = entity.hasCustomName()
            ? EnumChatFormatting.getTextWithoutFormattingCodes(entity.getCustomNameTag())
            : null;

        for (Entry entry : ENTITY_ENTRIES) {
            if (entry.entityId != null && (type == null || !type.equalsIgnoreCase(stripDomain(entry.entityId)))) {
                continue;
            }
            if (entry.name != null && (customName == null || !entry.name.matches(customName))) {
                continue;
            }
            return entry;
        }
        return null;
    }

    public static JemModel getModel(Entry entry) {
        JemModel model = MODELS.get(entry.modelPath);
        if (model != null) {
            return model;
        }
        Minecraft mc = Minecraft.getMinecraft();
        try {
            ResourceLocation location = new ResourceLocation("minecraft", BASE_PATH + entry.modelPath);
            IResource resource = mc.getResourceManager().getResource(location);
            JsonObject root = new JsonParser().parse(new InputStreamReader(resource.getInputStream(), "UTF-8"))
                .getAsJsonObject();
            model = JemParser.parse(root, BASE_PATH);
        } catch (Exception e) {
            Artemis.LOGGER.error("[Model] could not load {}", entry.modelPath, e);
            return null;
        }
        MODELS.put(entry.modelPath, model);
        return model;
    }

    private static String stripDomain(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? id : id.substring(colon + 1);
    }

    public static final class NameMatcher {
        private final Pattern pattern;
        private final String literal;
        private final boolean ignoreCase;

        private NameMatcher(Pattern pattern, String literal, boolean ignoreCase) {
            this.pattern = pattern;
            this.literal = literal;
            this.ignoreCase = ignoreCase;
        }

        public static NameMatcher compile(String value) {
            if (value.startsWith("regex:")) {
                return new NameMatcher(Pattern.compile(value.substring(6)), null, false);
            }
            if (value.startsWith("iregex:")) {
                return new NameMatcher(Pattern.compile(value.substring(7), Pattern.CASE_INSENSITIVE), null, true);
            }
            if (value.startsWith("pattern:")) {
                return new NameMatcher(Pattern.compile(wildcard(value.substring(8))), null, false);
            }
            if (value.startsWith("ipattern:")) {
                return new NameMatcher(Pattern.compile(wildcard(value.substring(9)), Pattern.CASE_INSENSITIVE),
                    null, true);
            }
            return new NameMatcher(null, value, false);
        }

        private static String wildcard(String value) {
            StringBuilder regex = new StringBuilder();
            for (int i = 0; i < value.length(); i++) {
                char c = value.charAt(i);
                if (c == '*') {
                    regex.append(".*");
                } else if (c == '?') {
                    regex.append('.');
                } else {
                    regex.append(Pattern.quote(String.valueOf(c)));
                }
            }
            return regex.toString();
        }

        public boolean matches(String value) {
            if (this.pattern != null) {
                return this.pattern.matcher(value).matches();
            }
            return this.ignoreCase ? this.literal.equalsIgnoreCase(value) : this.literal.equals(value);
        }
    }
}
