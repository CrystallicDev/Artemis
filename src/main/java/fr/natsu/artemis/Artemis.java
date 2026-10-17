package fr.natsu.artemis;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import fr.natsu.artemis.command.ArtemisCommand;
import fr.natsu.artemis.gui.ArtemisGuiController;
import fr.natsu.artemis.module.beam.BeamModule;
import fr.natsu.artemis.module.border.BorderModule;
import fr.natsu.artemis.module.cooldown.CooldownModule;
import fr.natsu.artemis.module.glow.GlowModule;
import fr.natsu.artemis.module.hologram.HologramModule;
import fr.natsu.artemis.module.nametag.NametagModule;
import fr.natsu.artemis.module.coloredfire.ColoredFireModule;
import fr.natsu.artemis.module.events.ArtemisEvents;
import fr.natsu.artemis.module.icon.IconRegistry;
import fr.natsu.artemis.module.limb.LimbModule;
import fr.natsu.artemis.module.marker.MarkerModule;
import fr.natsu.artemis.module.model.ModelRegistry;
import fr.natsu.artemis.module.notification.NotificationModule;
import fr.natsu.artemis.module.team.TeamModule;
import fr.natsu.artemis.module.vignette.VignetteModule;
import fr.natsu.artemis.module.waypoint.WaypointModule;
import fr.natsu.artemis.net.ApolloNetwork;
import fr.natsu.artemis.render.BeamRenderer;
import fr.natsu.artemis.render.BorderRenderer;
import fr.natsu.artemis.render.BossBarRenderer;
import fr.natsu.artemis.render.CooldownRenderer;
import fr.natsu.artemis.render.CustomModelRenderer;
import fr.natsu.artemis.render.GlowOverlayListener;
import fr.natsu.artemis.render.HologramRenderer;
import fr.natsu.artemis.render.LightningRenderer;
import fr.natsu.artemis.render.MarkerRenderer;
import fr.natsu.artemis.render.NametagRenderer;
import fr.natsu.artemis.render.NotificationRenderer;
import fr.natsu.artemis.render.TeamRenderer;
import fr.natsu.artemis.render.VignetteRenderer;
import fr.natsu.artemis.render.WaypointRenderer;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;

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
        ApolloNetwork network = ApolloNetwork.getInstance();
        MinecraftForge.EVENT_BUS.register(network);
        boolean lunar = LunarHost.isLunar();

        GlowModule.register(network);
        MinecraftForge.EVENT_BUS.register(new GlowOverlayListener());

        ModelRegistry.init();
        IconRegistry.init();
        MinecraftForge.EVENT_BUS.register(new CustomModelRenderer());
        MinecraftForge.EVENT_BUS.register(new BossBarRenderer());
        MinecraftForge.EVENT_BUS.register(new LightningRenderer());

        if (lunar) {
            LOGGER.info("[Artemis] init - Lunar profile: Glow/Chat/Lightning/Model/Icon/BossBar modules active");
            return;
        }

        NametagModule.register(network);
        MinecraftForge.EVENT_BUS.register(new NametagRenderer());

        CooldownModule.register(network);
        MinecraftForge.EVENT_BUS.register(new CooldownRenderer());

        VignetteModule.register(network);
        MinecraftForge.EVENT_BUS.register(new VignetteRenderer());

        TeamModule.register(network);
        MinecraftForge.EVENT_BUS.register(new TeamRenderer());

        WaypointModule.register(network);
        MinecraftForge.EVENT_BUS.register(new WaypointRenderer());

        MarkerModule.register(network);
        MinecraftForge.EVENT_BUS.register(new MarkerRenderer());

        BeamModule.register(network);
        MinecraftForge.EVENT_BUS.register(new BeamRenderer());

        NotificationModule.register(network);
        MinecraftForge.EVENT_BUS.register(new NotificationRenderer());

        BorderModule.register(network);
        MinecraftForge.EVENT_BUS.register(new BorderRenderer());

        HologramModule.register(network);
        MinecraftForge.EVENT_BUS.register(new HologramRenderer());

        ColoredFireModule.register(network);
        LimbModule.register(network);

        MinecraftForge.EVENT_BUS.register(new ArtemisEvents());

        ClientCommandHandler.instance.registerCommand(new ArtemisCommand());
        MinecraftForge.EVENT_BUS.register(new ArtemisGuiController());

        LOGGER.info("[Artemis] init - Apollo network + Glow/Nametag/Cooldown/Vignette/TeamView/Waypoint/Marker/Beam/Notification/Border/Hologram/Model modules active");
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        MixinAudit.report();
        LOGGER.info("[Artemis] postInit - ready");
    }
}
