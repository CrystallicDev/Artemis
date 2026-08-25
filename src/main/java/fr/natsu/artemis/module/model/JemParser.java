package fr.natsu.artemis.module.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import net.minecraft.util.ResourceLocation;

public final class JemParser {
    private static final String[][] FACE_KEYS = {
        {"uvDown"},
        {"uvUp"},
        {"uvNorth", "uvFront"},
        {"uvSouth", "uvBack"},
        {"uvWest", "uvLeft"},
        {"uvEast", "uvRight"}
    };

    private JemParser() {
    }

    public static JemModel parse(JsonObject root, String basePath) {
        JemModel model = new JemModel();
        model.texture = resolveTexture(string(root, "texture", null), basePath);

        float[] size = floatArray(root.get("textureSize"), 2, null);
        if (size != null) {
            model.textureWidth = size[0];
            model.textureHeight = size[1];
        }
        model.shadowSize = number(root, "shadowSize", 1.0F);

        JsonArray models = root.getAsJsonArray("models");
        if (models == null) {
            return model;
        }
        for (JsonElement element : models) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject entry = element.getAsJsonObject();
            JemPart part = parsePart(entry, basePath, model.texture,
                model.textureWidth, model.textureHeight);
            part.attachTo = string(entry, "part", null);
            part.attach = bool(entry, "attach", false);
            model.addPart(part);
        }
        return model;
    }

    private static JemPart parsePart(JsonObject element, String basePath, ResourceLocation parentTexture,
                                     float parentTextureWidth, float parentTextureHeight) {
        JemPart part = new JemPart();
        part.id = string(element, "id", "");
        part.scale = number(element, "scale", 1.0F);

        ResourceLocation texture = resolveTexture(string(element, "texture", null), basePath);
        if (texture != null) {
            part.texture = texture;
        }

        float textureWidth = parentTextureWidth;
        float textureHeight = parentTextureHeight;
        float[] size = floatArray(element.get("textureSize"), 2, null);
        if (size != null) {
            textureWidth = size[0];
            textureHeight = size[1];
        }

        String invertAxis = string(element, "invertAxis", "").toLowerCase();
        boolean invertX = invertAxis.indexOf('x') >= 0;
        boolean invertY = invertAxis.indexOf('y') >= 0;
        boolean invertZ = invertAxis.indexOf('z') >= 0;

        float[] translate = floatArray(element.get("translate"), 3, new float[3]);
        part.translateX = invertX ? -translate[0] : translate[0];
        part.translateY = invertY ? -translate[1] : translate[1];
        part.translateZ = invertZ ? -translate[2] : translate[2];

        float[] rotate = floatArray(element.get("rotate"), 3, new float[3]);
        float rx = (float) Math.toRadians(rotate[0]);
        float ry = (float) Math.toRadians(rotate[1]);
        float rz = (float) Math.toRadians(rotate[2]);
        part.rotateX = invertX ? -rx : rx;
        part.rotateY = invertY ? -ry : ry;
        part.rotateZ = invertZ ? -rz : rz;

        String mirror = string(element, "mirrorTexture", "").toLowerCase();
        boolean mirrorU = mirror.indexOf('u') >= 0;
        boolean mirrorV = mirror.indexOf('v') >= 0;

        parseBoxes(element.getAsJsonArray("boxes"), part, invertX, invertY, invertZ,
            textureWidth, textureHeight, mirrorU, mirrorV);
        parseBoxes(element.getAsJsonArray("sprites"), part, invertX, invertY, invertZ,
            textureWidth, textureHeight, mirrorU, mirrorV);

        ResourceLocation childTexture = part.texture != null ? part.texture : parentTexture;
        if (element.has("submodel") && element.get("submodel").isJsonObject()) {
            part.addChild(parsePart(element.getAsJsonObject("submodel"), basePath, childTexture,
                textureWidth, textureHeight));
        }
        JsonArray submodels = element.getAsJsonArray("submodels");
        if (submodels != null) {
            for (JsonElement child : submodels) {
                if (child.isJsonObject()) {
                    part.addChild(parsePart(child.getAsJsonObject(), basePath, childTexture,
                        textureWidth, textureHeight));
                }
            }
        }
        return part;
    }

    private static void parseBoxes(JsonArray boxes, JemPart part, boolean invertX, boolean invertY,
                                   boolean invertZ, float textureWidth, float textureHeight,
                                   boolean mirrorU, boolean mirrorV) {
        if (boxes == null) {
            return;
        }
        for (JsonElement element : boxes) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject box = element.getAsJsonObject();
            float[] coordinates = floatArray(box.get("coordinates"), 6, null);
            if (coordinates == null) {
                continue;
            }
            if (invertX) {
                coordinates[0] = -coordinates[0] - coordinates[3];
            }
            if (invertY) {
                coordinates[1] = -coordinates[1] - coordinates[4];
            }
            if (invertZ) {
                coordinates[2] = -coordinates[2] - coordinates[5];
            }

            float[] offset = floatArray(box.get("textureOffset"), 2, null);
            int[] textureOffset = offset == null ? null : new int[] {(int) offset[0], (int) offset[1]};
            int[][] faceUvs = parseFaceUvs(box);
            if (textureOffset == null && faceUvs == null) {
                continue;
            }
            part.addBox(new JemBox(coordinates, number(box, "sizeAdd", 0.0F), textureOffset, faceUvs,
                textureWidth, textureHeight, mirrorU, mirrorV));
        }
    }

    private static int[][] parseFaceUvs(JsonObject box) {
        int[][] faces = null;
        for (int face = 0; face < FACE_KEYS.length; face++) {
            float[] uv = null;
            for (String key : FACE_KEYS[face]) {
                uv = floatArray(box.get(key), 4, null);
                if (uv != null) {
                    break;
                }
            }
            if (uv == null) {
                continue;
            }
            if (faces == null) {
                faces = new int[6][];
            }
            faces[face] = new int[] {(int) uv[0], (int) uv[1], (int) uv[2], (int) uv[3]};
        }
        return faces;
    }

    private static ResourceLocation resolveTexture(String value, String basePath) {
        if (value == null || value.isEmpty()) {
            return null;
        }
        String path = value.endsWith(".png") ? value : value + ".png";
        if (path.indexOf(':') >= 0) {
            return new ResourceLocation(path);
        }
        if (path.charAt(0) == '/') {
            return new ResourceLocation(path.substring(1));
        }
        if (path.indexOf('/') >= 0) {
            return new ResourceLocation(path);
        }
        return new ResourceLocation(basePath + path);
    }

    private static String string(JsonObject object, String key, String fallback) {
        JsonElement element = object.get(key);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsString();
    }

    private static float number(JsonObject object, String key, float fallback) {
        JsonElement element = object.get(key);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsFloat();
    }

    private static boolean bool(JsonObject object, String key, boolean fallback) {
        JsonElement element = object.get(key);
        return element == null || !element.isJsonPrimitive() ? fallback : element.getAsBoolean();
    }

    private static float[] floatArray(JsonElement element, int length, float[] fallback) {
        if (element == null || !element.isJsonArray()) {
            return fallback;
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() < length) {
            return fallback;
        }
        float[] values = new float[length];
        for (int i = 0; i < length; i++) {
            values[i] = array.get(i).getAsFloat();
        }
        return values;
    }
}
