package fr.natsu.artemis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.natsu.artemis.command.ArtemisCommand;
import fr.natsu.artemis.gui.ArtemisGuiController;
import fr.natsu.artemis.module.cooldown.CooldownModule;
import fr.natsu.artemis.module.glow.GlowModule;
import fr.natsu.artemis.module.nametag.NametagModule;
import fr.natsu.artemis.module.coloredfire.ColoredFireModule;
import fr.natsu.artemis.module.limb.LimbModule;
import fr.natsu.artemis.module.marker.MarkerModule;
import fr.natsu.artemis.module.team.TeamModule;
import fr.natsu.artemis.module.vignette.VignetteModule;
import fr.natsu.artemis.module.waypoint.WaypointModule;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.render.CooldownRenderer;
import fr.natsu.artemis.render.GlowOverlayListener;
import fr.natsu.artemis.render.LightningRenderer;
import fr.natsu.artemis.render.MarkerRenderer;
import fr.natsu.artemis.render.NametagRenderer;
import fr.natsu.artemis.render.TeamRenderer;
import fr.natsu.artemis.render.VignetteRenderer;
import fr.natsu.artemis.render.WaypointRenderer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

/**
 * Artemis entry point.
 *
 * <p>Artemis is a <b>client-only</b> Forge 1.8.9 mod that bridges Apollo (Lunar Client's server API)
 * and Forge: when an Apollo server asks the client to enable an effect (Glowing, TeamView, Cooldown,
 * Vignette, ...), Artemis handles it the way a real Lunar client would.</p>
 *
 * <p>The Apollo protocol travels over the {@code lunar:apollo} plugin-message channel as
 * {@code google.protobuf.Any} messages. The network layer registers that channel, parses the messages
 * and dispatches them to the modules.</p>
 */
@Mod(
    modid = Artemis.MOD_ID,
    name = Artemis.MOD_NAME,
    version = Artemis.VERSION,
    clientSideOnly = true,
    acceptedMinecraftVersions = "[1.8.9]"
)
public final class Artemis {

    public static final String MOD_ID = "artemis";
    public static final String MOD_NAME = "Artemis";
    public static final String VERSION = "0.1.0";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        ArtemisConfig.init(event.getModConfigurationDirectory());
        LOGGER.info("[Artemis] preInit - Forge -> Apollo (Lunar Client) bridge for 1.8.9");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Apollo network layer: registers `lunar:apollo` at login, parses the incoming `Any` messages
        // and dispatches them to the modules.
        ApolloNetwork network = ApolloNetwork.getInstance();
        MinecraftForge.EVENT_BUS.register(network);

        // Glowing: network handlers + outline rendering.
        GlowModule.register(network);
        MinecraftForge.EVENT_BUS.register(new GlowOverlayListener());

        // Nametag: network handlers + rendering above players.
        NametagModule.register(network);
        MinecraftForge.EVENT_BUS.register(new NametagRenderer());

        // Cooldown: network handlers + HUD rendering.
        CooldownModule.register(network);
        MinecraftForge.EVENT_BUS.register(new CooldownRenderer());

        // Vignette: network handlers + full-screen overlay.
        VignetteModule.register(network);
        MinecraftForge.EVENT_BUS.register(new VignetteRenderer());

        // TeamView: network handlers + world-space markers.
        TeamModule.register(network);
        MinecraftForge.EVENT_BUS.register(new TeamRenderer());

        // Waypoint: network handlers + world-space beam/label.
        WaypointModule.register(network);
        MinecraftForge.EVENT_BUS.register(new WaypointRenderer());

        // Marker: network handlers + conditional icon/info (hover).
        MarkerModule.register(network);
        MinecraftForge.EVENT_BUS.register(new MarkerRenderer());

        // Colored Fire: per-player flame color (rendered via MixinRenderFire).
        ColoredFireModule.register(network);

        // Limb: per-player body-part hiding (rendered via MixinModelPlayer).
        LimbModule.register(network);

        // Inventory (interaction): items tagged lunar.* are handled by MixinGuiContainer /
        // MixinGuiScreen, nothing to register here.

        // Custom chat: received on artemis:chat (MixinNetworkManager) -> injected into the vanilla
        // chat, hex-rendered via MixinFontRenderer. Nothing to register here.

        // Colored lightning: received on artemis:lightning (MixinNetworkManager) -> LightningState,
        // drawn on RenderWorldLastEvent. A non-Apollo feature (like the chat).
        MinecraftForge.EVENT_BUS.register(new LightningRenderer());

        // Client command /artemis + deferred opening of the config screen (on tick).
        ClientCommandHandler.instance.registerCommand(new ArtemisCommand());
        MinecraftForge.EVENT_BUS.register(new ArtemisGuiController());

        LOGGER.info("[Artemis] init - Apollo network + Glow/Nametag/Cooldown/Vignette/TeamView/Waypoint/Marker modules active");

        // The Glint module (per-item render via the lunar.glint NBT) is active through MixinRenderItem.
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("[Artemis] postInit - ready");
    }
}
