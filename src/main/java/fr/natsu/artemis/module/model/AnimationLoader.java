package fr.natsu.artemis.module.model;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public final class AnimationLoader {

    private AnimationLoader() {
    }

    public static JemAnimation load(InputStream stream, String wanted) throws Exception {
        JsonObject root = new JsonParser().parse(new InputStreamReader(stream, "UTF-8")).getAsJsonObject();
        JsonObject animations = root.getAsJsonObject("animations");
        if (animations == null || animations.entrySet().isEmpty()) {
            return null;
        }

        Map.Entry<String, JsonElement> chosen = null;
        if (wanted != null && !wanted.isEmpty()) {
            for (Map.Entry<String, JsonElement> entry : animations.entrySet()) {
                if (entry.getKey().equals(wanted) || entry.getKey().endsWith("." + wanted)) {
                    chosen = entry;
                    break;
                }
            }
            if (chosen == null) {
                return null;
            }
        } else {
            chosen = animations.entrySet().iterator().next();
        }
        return parse(chosen.getKey(), chosen.getValue().getAsJsonObject());
    }

    private static JemAnimation parse(String name, JsonObject animation) {
        double length = animation.has("animation_length")
            ? animation.get("animation_length").getAsDouble() : 0.0D;
        boolean loop = animation.has("loop") && isTrue(animation.get("loop"));

        Map<String, JemAnimation.Timeline> bones = new HashMap<>();
        JsonObject boneObjects = animation.getAsJsonObject("bones");
        if (boneObjects != null) {
            for (Map.Entry<String, JsonElement> entry : boneObjects.entrySet()) {
                if (!entry.getValue().isJsonObject()) {
                    continue;
                }
                JsonObject bone = entry.getValue().getAsJsonObject();
                JemAnimation.Timeline timeline = new JemAnimation.Timeline();
                readChannel(bone.get("rotation"), timeline.rotation);
                readChannel(bone.get("position"), timeline.position);
                readChannel(bone.get("scale"), timeline.scale);
                readChannel(bone.get("visible"), timeline.visible);
                if (!timeline.rotation.isEmpty() || !timeline.position.isEmpty()
                    || !timeline.scale.isEmpty() || !timeline.visible.isEmpty()) {
                    bones.put(entry.getKey(), timeline);
                }
            }
        }
        return new JemAnimation(name, length, loop, bones);
    }

    private static boolean isTrue(JsonElement element) {
        try {
            return element.getAsBoolean();
        } catch (Exception e) {
            return "true".equals(element.getAsString());
        }
    }

    private static void readChannel(JsonElement channel, TreeMap<Double, JemAnimation.Keyframe> out) {
        if (channel == null) {
            return;
        }
        if (channel.isJsonObject()) {
            JsonObject object = channel.getAsJsonObject();
            boolean timed = true;
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                try {
                    Double.parseDouble(entry.getKey());
                } catch (NumberFormatException e) {
                    timed = false;
                    break;
                }
            }
            if (timed) {
                for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                    JemAnimation.Keyframe keyframe = readKeyframe(entry.getValue());
                    if (keyframe != null) {
                        out.put(Double.parseDouble(entry.getKey()), keyframe);
                    }
                }
                return;
            }
        }
        JemAnimation.Keyframe keyframe = readKeyframe(channel);
        if (keyframe != null) {
            out.put(0.0D, keyframe);
        }
    }

    private static JemAnimation.Keyframe readKeyframe(JsonElement element) {
        double[] values = toTriple(element);
        if (values == null) {
            return null;
        }
        String easing = null;
        boolean catmullrom = false;
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("easing")) {
                easing = object.get("easing").getAsString();
            }
            if (object.has("lerp_mode")) {
                catmullrom = "catmullrom".equals(object.get("lerp_mode").getAsString());
            }
        }
        return new JemAnimation.Keyframe(values, easing, catmullrom);
    }

    private static double[] toTriple(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return null;
        }
        if (element.isJsonPrimitive()) {
            try {
                double value = element.getAsDouble();
                return new double[] {value, value, value};
            } catch (Exception e) {
                return isTrue(element) ? new double[] {1.0D, 1.0D, 1.0D} : new double[] {0.0D, 0.0D, 0.0D};
            }
        }
        if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            if (array.size() == 0) {
                return null;
            }
            if (array.get(0).isJsonObject()) {
                return toTriple(array.get(0));
            }
            if (array.size() >= 3) {
                return new double[] {
                    array.get(0).getAsDouble(), array.get(1).getAsDouble(), array.get(2).getAsDouble()
                };
            }
            double value = array.get(0).getAsDouble();
            return new double[] {value, value, value};
        }
        if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("vector")) {
                return toTriple(object.get("vector"));
            }
            if (object.has("post")) {
                return toTriple(object.get("post"));
            }
            if (object.has("pre")) {
                return toTriple(object.get("pre"));
            }
        }
        return null;
    }
}
