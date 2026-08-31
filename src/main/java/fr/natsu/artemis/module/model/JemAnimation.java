package fr.natsu.artemis.module.model;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public final class JemAnimation {

    public static final class Keyframe {
        public final double[] value;
        public final String easing;
        public final boolean catmullrom;

        public Keyframe(double[] value, String easing, boolean catmullrom) {
            this.value = value;
            this.easing = easing;
            this.catmullrom = catmullrom;
        }
    }

    public static final class Timeline {
        public final TreeMap<Double, Keyframe> rotation = new TreeMap<>();
        public final TreeMap<Double, Keyframe> position = new TreeMap<>();
        public final TreeMap<Double, Keyframe> scale = new TreeMap<>();
        public final TreeMap<Double, Keyframe> visible = new TreeMap<>();
    }

    private final String name;
    private final double length;
    private final boolean loop;
    private final Map<String, Timeline> bones;

    public JemAnimation(String name, double length, boolean loop, Map<String, Timeline> bones) {
        this.name = name;
        this.length = length;
        this.loop = loop;
        this.bones = bones;
    }

    public String name() {
        return this.name;
    }

    public double length() {
        return this.length;
    }

    public boolean loop() {
        return this.loop;
    }

    public Set<String> boneNames() {
        return Collections.unmodifiableSet(this.bones.keySet());
    }

    public double time(double seconds) {
        if (this.length <= 0.0D) {
            return 0.0D;
        }
        return this.loop ? seconds % this.length : Math.min(seconds, this.length);
    }

    public double[] sampleRotation(String bone, double time) {
        Timeline timeline = this.bones.get(bone);
        return timeline == null ? null : sample(timeline.rotation, time);
    }

    public double[] samplePosition(String bone, double time) {
        Timeline timeline = this.bones.get(bone);
        return timeline == null ? null : sample(timeline.position, time);
    }

    public double[] sampleScale(String bone, double time) {
        Timeline timeline = this.bones.get(bone);
        return timeline == null ? null : sample(timeline.scale, time);
    }

    public Boolean sampleVisible(String bone, double time) {
        Timeline timeline = this.bones.get(bone);
        if (timeline == null || timeline.visible.isEmpty()) {
            return null;
        }
        Map.Entry<Double, Keyframe> entry = timeline.visible.floorEntry(time);
        if (entry == null) {
            entry = timeline.visible.ceilingEntry(time);
        }
        return entry.getValue().value[0] >= 0.5D;
    }

    private static double[] sample(TreeMap<Double, Keyframe> timeline, double time) {
        if (timeline.isEmpty()) {
            return null;
        }
        Map.Entry<Double, Keyframe> low = timeline.floorEntry(time);
        Map.Entry<Double, Keyframe> high = timeline.ceilingEntry(time);
        if (low == null) {
            return high.getValue().value.clone();
        }
        if (high == null) {
            return low.getValue().value.clone();
        }

        double lowTime = low.getKey();
        double highTime = high.getKey();
        Keyframe from = low.getValue();
        Keyframe to = high.getValue();
        if (highTime - lowTime < 1.0E-9D) {
            return to.value.clone();
        }

        double fraction = (time - lowTime) / (highTime - lowTime);
        if (to.catmullrom) {
            Double beforeKey = timeline.lowerKey(lowTime);
            Double afterKey = timeline.higherKey(highTime);
            double[] before = beforeKey == null ? from.value : timeline.get(beforeKey).value;
            double[] after = afterKey == null ? to.value : timeline.get(afterKey).value;
            return catmullRom(before, from.value, to.value, after, fraction);
        }

        double eased = ease(to.easing, fraction);
        return new double[] {
            from.value[0] + (to.value[0] - from.value[0]) * eased,
            from.value[1] + (to.value[1] - from.value[1]) * eased,
            from.value[2] + (to.value[2] - from.value[2]) * eased
        };
    }

    private static double ease(String type, double f) {
        if (type == null || type.equals("linear")) {
            return f;
        }
        if (type.equals("step")) {
            return 0.0D;
        }
        final double c1 = 1.70158D;
        final double c3 = c1 + 1.0D;
        final double c2 = c1 * 1.525D;
        if (type.equals("easeInBack")) {
            return c3 * f * f * f - c1 * f * f;
        }
        if (type.equals("easeOutBack")) {
            double g = f - 1.0D;
            return 1.0D + c3 * g * g * g + c1 * g * g;
        }
        if (type.equals("easeInOutBack")) {
            if (f < 0.5D) {
                return (Math.pow(2.0D * f, 2.0D) * ((c2 + 1.0D) * 2.0D * f - c2)) / 2.0D;
            }
            return (Math.pow(2.0D * f - 2.0D, 2.0D) * ((c2 + 1.0D) * (f * 2.0D - 2.0D) + c2) + 2.0D) / 2.0D;
        }
        if (type.equals("easeInSine")) {
            return 1.0D - Math.cos((f * Math.PI) / 2.0D);
        }
        if (type.equals("easeOutSine")) {
            return Math.sin((f * Math.PI) / 2.0D);
        }
        if (type.equals("easeInOutSine")) {
            return -(Math.cos(Math.PI * f) - 1.0D) / 2.0D;
        }
        return f;
    }

    private static double[] catmullRom(double[] p0, double[] p1, double[] p2, double[] p3, double f) {
        double f2 = f * f;
        double f3 = f2 * f;
        double[] out = new double[3];
        for (int i = 0; i < 3; i++) {
            out[i] = 0.5D * ((2.0D * p1[i]) + (-p0[i] + p2[i]) * f
                + (2.0D * p0[i] - 5.0D * p1[i] + 4.0D * p2[i] - p3[i]) * f2
                + (-p0[i] + 3.0D * p1[i] - 3.0D * p2[i] + p3[i]) * f3);
        }
        return out;
    }
}
