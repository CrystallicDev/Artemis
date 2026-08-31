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
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;

public final class ModelRegistry {

    private static final String BASE_PATH = "artemis/cem/";
    private static final ResourceLocation INDEX = new ResourceLocation("minecraft", BASE_PATH + "index.json");

    private static final List<Entry> ENTITY_ENTRIES = new ArrayList<>();
    private static final List<ItemEntry> ITEM_ENTRIES = new ArrayList<>();
    private static final List<BossBarEntry> BOSSBAR_ENTRIES = new ArrayList<>();
    private static final Map<String, JemModel> MODELS = new HashMap<>();
    private static final Map<String, JemAnimation> ANIMATIONS = new HashMap<>();

    private static boolean loadAttempted;

    public static final class Entry {
        public final String modelPath;
        public final String entityId;
        public final NameMatcher name;
        public final float scale;
        public final boolean ignoreCull;
        public final float renderDistance;
        public final String animationPath;
        public final String animationName;

        Entry(String modelPath, String entityId, NameMatcher name, float scale,
              boolean ignoreCull, float renderDistance, String animationPath, String animationName) {
            this.modelPath = modelPath;
            this.entityId = entityId;
            this.name = name;
            this.scale = scale;
            this.ignoreCull = ignoreCull;
            this.renderDistance = renderDistance;
            this.animationPath = animationPath;
            this.animationName = animationName;
        }
    }

    public static final class ItemEntry {
        public final String modelPath;
        public final String itemId;
        public final NameMatcher name;
        public final float scale;
        public final float[] offset;
        public final String attach;
        public final String animationPath;
        public final String animationName;

        ItemEntry(String modelPath, String itemId, NameMatcher name, float scale, float[] offset,
                  String attach, String animationPath, String animationName) {
            this.modelPath = modelPath;
            this.itemId = itemId;
            this.name = name;
            this.scale = scale;
            this.offset = offset;
            this.attach = attach;
            this.animationPath = animationPath;
            this.animationName = animationName;
        }
    }

    public static final class BossBarEntry {
        public final NameMatcher name;
        public final String texturePath;
        public final int color;
        public final int backgroundColor;
        public final int textColor;
        public final int notches;
        public final boolean hideName;
        public final int y;

        BossBarEntry(NameMatcher name, String texturePath, int color, int backgroundColor,
                     int textColor, int notches, boolean hideName, int y) {
            this.name = name;
            this.texturePath = texturePath;
            this.color = color;
            this.backgroundColor = backgroundColor;
            this.textColor = textColor;
            this.notches = notches;
            this.hideName = hideName;
            this.y = y;
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
            Artemis.LOGGER.info("[Model] reload listener registered on {}",
                resourceManager.getClass().getSimpleName());
        } else {
            Artemis.LOGGER.warn("[Model] resource manager is not reloadable ({}), loading once",
                resourceManager.getClass().getSimpleName());
            reload(resourceManager);
        }
    }

    public static void ensureLoaded() {
        if (loadAttempted) {
            return;
        }
        reload(Minecraft.getMinecraft().getResourceManager());
    }

    public static void reload(IResourceManager resourceManager) {
        for (JemModel model : MODELS.values()) {
            model.dispose();
        }
        MODELS.clear();
        ANIMATIONS.clear();
        ENTITY_ENTRIES.clear();
        ITEM_ENTRIES.clear();
        BOSSBAR_ENTRIES.clear();

        loadAttempted = true;
        JsonObject index;
        try {
            IResource resource = resourceManager.getResource(INDEX);
            index = new JsonParser().parse(new InputStreamReader(resource.getInputStream(), "UTF-8"))
                .getAsJsonObject();
        } catch (java.io.IOException e) {
            Artemis.LOGGER.info("[Model] no '{}' found in the loaded resource packs", INDEX);
            return;
        } catch (Exception e) {
            Artemis.LOGGER.error("[Model] could not parse '{}'", INDEX, e);
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

        JsonArray items = index.getAsJsonArray("items");
        if (items != null) {
            for (JsonElement element : items) {
                if (element.isJsonObject()) {
                    ItemEntry entry = parseItemEntry(element.getAsJsonObject());
                    if (entry != null) {
                        ITEM_ENTRIES.add(entry);
                    }
                }
            }
        }

        JsonArray bossbars = index.getAsJsonArray("bossbars");
        if (bossbars != null) {
            for (JsonElement element : bossbars) {
                if (element.isJsonObject()) {
                    BossBarEntry entry = parseBossBarEntry(element.getAsJsonObject());
                    if (entry != null) {
                        BOSSBAR_ENTRIES.add(entry);
                    }
                }
            }
        }

        Artemis.LOGGER.info("[Model] loaded {} entity, {} item, {} boss bar override(s)",
            ENTITY_ENTRIES.size(), ITEM_ENTRIES.size(), BOSSBAR_ENTRIES.size());
        for (Entry entry : ENTITY_ENTRIES) {
            Artemis.LOGGER.info("[Model]   entity model={} entity={} name={}",
                entry.modelPath, entry.entityId, entry.name);
        }
        for (ItemEntry entry : ITEM_ENTRIES) {
            Artemis.LOGGER.info("[Model]   item model={} item={} name={} attach={}",
                entry.modelPath, entry.itemId, entry.name, entry.attach);
        }
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
        JsonElement animation = object.get("animation");
        JsonElement animationName = object.get("animationName");
        return new Entry(model.getAsString(),
            entity == null ? null : entity.getAsString(),
            name == null ? null : NameMatcher.compile(name.getAsString()),
            scale == null ? 1.0F : scale.getAsFloat(),
            ignoreCull != null && ignoreCull.getAsBoolean(),
            renderDistance == null ? 0.0F : renderDistance.getAsFloat(),
            animation == null ? null : animation.getAsString(),
            animationName == null ? null : animationName.getAsString());
    }

    private static ItemEntry parseItemEntry(JsonObject object) {
        JsonElement model = object.get("model");
        if (model == null || !model.isJsonPrimitive()) {
            return null;
        }
        JsonElement item = object.get("item");
        JsonElement name = object.get("name");
        JsonElement scale = object.get("scale");
        JsonElement attach = object.get("attach");
        JsonElement animation = object.get("animation");
        JsonElement animationName = object.get("animationName");

        float[] offset = new float[3];
        JsonElement offsetElement = object.get("offset");
        if (offsetElement != null && offsetElement.isJsonArray()
            && offsetElement.getAsJsonArray().size() >= 3) {
            JsonArray array = offsetElement.getAsJsonArray();
            for (int i = 0; i < 3; i++) {
                offset[i] = array.get(i).getAsFloat();
            }
        }

        return new ItemEntry(model.getAsString(),
            item == null ? null : item.getAsString(),
            name == null ? null : NameMatcher.compile(name.getAsString()),
            scale == null ? 1.0F : scale.getAsFloat(),
            offset,
            attach == null ? null : attach.getAsString(),
            animation == null ? null : animation.getAsString(),
            animationName == null ? null : animationName.getAsString());
    }

    private static BossBarEntry parseBossBarEntry(JsonObject object) {
        JsonElement name = object.get("name");
        if (name == null || !name.isJsonPrimitive()) {
            return null;
        }
        JsonElement texture = object.get("texture");
        JsonElement notches = object.get("notches");
        JsonElement hideName = object.get("hideName");
        JsonElement y = object.get("y");
        return new BossBarEntry(NameMatcher.compile(name.getAsString()),
            texture == null ? null : texture.getAsString(),
            color(object, "color", 0xFFFFFFFF),
            color(object, "backgroundColor", 0xFF202020),
            color(object, "textColor", 0xFFFFFFFF),
            notches == null ? 0 : notches.getAsInt(),
            hideName != null && hideName.getAsBoolean(),
            y == null ? 12 : y.getAsInt());
    }

    private static int color(JsonObject object, String key, int fallback) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive()) {
            return fallback;
        }
        String value = element.getAsString().trim();
        if (value.startsWith("#")) {
            value = value.substring(1);
        }
        try {
            long parsed = Long.parseLong(value, 16);
            return value.length() <= 6 ? (int) (0xFF000000L | parsed) : (int) parsed;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    public static boolean hasBossBars() {
        return !BOSSBAR_ENTRIES.isEmpty();
    }

    public static BossBarEntry findForBossBar(String bossName) {
        if (BOSSBAR_ENTRIES.isEmpty() || bossName == null) {
            return null;
        }
        String plain = EnumChatFormatting.getTextWithoutFormattingCodes(bossName);
        for (BossBarEntry entry : BOSSBAR_ENTRIES) {
            if (entry.name.matches(plain)) {
                return entry;
            }
        }
        return null;
    }

    public static boolean isEmpty() {
        return ENTITY_ENTRIES.isEmpty();
    }

    public static boolean hasItems() {
        return !ITEM_ENTRIES.isEmpty();
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

    public static ItemEntry findForItem(ItemStack stack) {
        if (ITEM_ENTRIES.isEmpty() || stack == null || stack.getItem() == null) {
            return null;
        }
        Object registryName = Item.itemRegistry.getNameForObject(stack.getItem());
        String itemId = registryName == null ? null : registryName.toString();
        String displayName = stack.hasDisplayName()
            ? EnumChatFormatting.getTextWithoutFormattingCodes(stack.getDisplayName()) : null;

        for (ItemEntry entry : ITEM_ENTRIES) {
            if (entry.itemId != null && (itemId == null || !matchesId(itemId, entry.itemId))) {
                continue;
            }
            if (entry.name != null && (displayName == null || !entry.name.matches(displayName))) {
                continue;
            }
            return entry;
        }
        return null;
    }

    private static boolean matchesId(String actual, String wanted) {
        return actual.equalsIgnoreCase(wanted)
            || stripDomain(actual).equalsIgnoreCase(stripDomain(wanted));
    }

    public static JemModel getModel(Entry entry) {
        return getModel(entry.modelPath);
    }

    public static JemModel getModel(String modelPath) {
        JemModel model = MODELS.get(modelPath);
        if (model != null) {
            return model;
        }
        try {
            ResourceLocation location = new ResourceLocation("minecraft", BASE_PATH + modelPath);
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            JsonObject root = new JsonParser().parse(new InputStreamReader(resource.getInputStream(), "UTF-8"))
                .getAsJsonObject();
            model = JemParser.parse(root, BASE_PATH);
        } catch (Exception e) {
            Artemis.LOGGER.error("[Model] could not load {}", modelPath, e);
            return null;
        }
        MODELS.put(modelPath, model);
        return model;
    }

    public static JemAnimation getAnimation(Entry entry) {
        return getAnimation(entry.animationPath, entry.animationName);
    }

    public static JemAnimation getAnimation(String animationPath, String wantedName) {
        if (animationPath == null) {
            return null;
        }
        String key = animationPath + "#" + wantedName;
        if (ANIMATIONS.containsKey(key)) {
            return ANIMATIONS.get(key);
        }
        JemAnimation animation = null;
        try {
            ResourceLocation location = new ResourceLocation("minecraft", BASE_PATH + animationPath);
            IResource resource = Minecraft.getMinecraft().getResourceManager().getResource(location);
            animation = AnimationLoader.load(resource.getInputStream(), wantedName);
            if (animation == null) {
                Artemis.LOGGER.warn("[Model] animation '{}' not found in {}", wantedName, animationPath);
            } else {
                Artemis.LOGGER.info("[Model] loaded animation '{}' ({}s, loop={}, {} bones) from {}",
                    animation.name(), animation.length(), animation.loop(),
                    animation.boneNames().size(), animationPath);
            }
        } catch (Exception e) {
            Artemis.LOGGER.error("[Model] could not load animation {}", animationPath, e);
        }
        ANIMATIONS.put(key, animation);
        return animation;
    }

    public static ResourceLocation resource(String path) {
        return new ResourceLocation("minecraft", BASE_PATH + path);
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

        @Override
        public String toString() {
            return this.pattern != null ? this.pattern.pattern() : this.literal;
        }

        public boolean matches(String value) {
            if (this.pattern != null) {
                return this.pattern.matcher(value).matches();
            }
            return this.ignoreCase ? this.literal.equalsIgnoreCase(value) : this.literal.equals(value);
        }
    }
}
