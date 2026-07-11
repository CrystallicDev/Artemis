package fr.natsu.artemis.module.vignette;

import com.google.protobuf.Any;
import com.google.protobuf.CodedInputStream;
import com.google.protobuf.WireFormat;

import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.net.ApolloNetwork;
import net.minecraft.util.ResourceLocation;

/**
 * Module Vignette : traduit {@code lunarclient.apollo.vignette.v1.*} en état de {@link VignetteState}.
 * Rendu plein écran assuré par {@code VignetteRenderer}.
 *
 * <p>{@code DisplayVignetteMessage} : resource_location(#1 string), opacity(#2 float).</p>
 */
public final class VignetteModule {

    private static final String DISPLAY = "lunarclient.apollo.vignette.v1.DisplayVignetteMessage";
    private static final String RESET = "lunarclient.apollo.vignette.v1.ResetVignetteMessage";

    private VignetteModule() {
    }

    public static void register(ApolloNetwork network) {
        network.register(DISPLAY, VignetteModule::onDisplay);
        network.register(RESET, VignetteModule::onReset);
    }

    private static void onDisplay(Any message) throws Exception {
        CodedInputStream in = message.getValue().newCodedInput();
        String location = null;
        float opacity = 0.0F;
        int tag;
        while ((tag = in.readTag()) != 0) {
            switch (WireFormat.getTagFieldNumber(tag)) {
                case 1:
                    location = in.readString();
                    break;
                case 2:
                    opacity = in.readFloat();
                    break;
                default:
                    in.skipField(tag);
            }
        }

        if (location == null || location.isEmpty()) {
            VignetteState.reset();
            return;
        }

        VignetteState.display(new ResourceLocation(location), opacity);
        Artemis.LOGGER.info("[Vignette] display '{}' opacite={}", location, opacity);
    }

    private static void onReset(Any message) {
        VignetteState.reset();
        Artemis.LOGGER.info("[Vignette] reset");
    }
}
