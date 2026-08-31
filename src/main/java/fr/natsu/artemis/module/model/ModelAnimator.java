package fr.natsu.artemis.module.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import fr.natsu.artemis.Artemis;

public final class ModelAnimator {

    private static final double SIGN_X = -1.0D;
    private static final double SIGN_Y = -1.0D;
    private static final double SIGN_Z = 1.0D;

    private static final Set<String> REPORTED = new HashSet<>();

    private ModelAnimator() {
    }

    public static void apply(JemModel model, JemAnimation animation, double seconds) {
        double time = animation.time(seconds);
        int matched = 0;
        List<String> missing = new ArrayList<>();

        for (String bone : animation.boneNames()) {
            JemPart part = model.resolve(bone);
            if (part == null) {
                missing.add(bone);
                continue;
            }
            matched++;

            double[] rotation = animation.sampleRotation(bone, time);
            if (rotation != null) {
                part.animRotateX = (float) Math.toRadians(SIGN_X * rotation[0]);
                part.animRotateY = (float) Math.toRadians(SIGN_Y * rotation[1]);
                part.animRotateZ = (float) Math.toRadians(SIGN_Z * rotation[2]);
            }

            double[] position = animation.samplePosition(bone, time);
            if (position != null) {
                part.animTranslateX = (float) (SIGN_X * position[0]);
                part.animTranslateY = (float) (SIGN_Y * position[1]);
                part.animTranslateZ = (float) (SIGN_Z * position[2]);
            }

            double[] scale = animation.sampleScale(bone, time);
            if (scale != null) {
                part.animScaleX = (float) scale[0];
                part.animScaleY = (float) scale[1];
                part.animScaleZ = (float) scale[2];
            }

            Boolean visible = animation.sampleVisible(bone, time);
            if (visible != null) {
                part.hidden = !visible.booleanValue();
            }
        }

        report(model, animation, matched, missing);
    }

    private static void report(JemModel model, JemAnimation animation, int matched, List<String> missing) {
        String key = System.identityHashCode(model) + "/" + animation.name();
        if (!REPORTED.add(key)) {
            return;
        }
        int total = matched + missing.size();
        if (matched == 0) {
            Artemis.LOGGER.warn("[Model] animation '{}' matched 0 of {} bone(s): no part id matches",
                animation.name(), total);
        } else {
            Artemis.LOGGER.info("[Model] animation '{}' bound to {}/{} bone(s)",
                animation.name(), matched, total);
        }
        if (!missing.isEmpty()) {
            Artemis.LOGGER.warn("[Model] unmatched animation bone(s): {}",
                missing.size() > 8 ? missing.subList(0, 8) + " ..." : missing);
        }
    }
}
