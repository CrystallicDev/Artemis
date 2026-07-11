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
 * Point d'entrée d'Artemis.
 *
 * <p>Artemis est un mod Forge 1.8.9 <b>client uniquement</b> qui fait office de pont de
 * compatibilité entre Apollo (l'API serveur de Lunar Client) et Forge : lorsqu'un serveur
 * utilisant Apollo demande au client d'activer un effet (Glowing, TeamView, Cooldown,
 * Vignette, ...), Artemis traite la demande comme le ferait un vrai client Lunar.</p>
 *
 * <p>Le protocole Apollo transite sur le canal plugin-message {@code lunar:apollo} sous la
 * forme de messages protobuf {@code google.protobuf.Any}. La couche réseau qui enregistre ce
 * canal, parse les messages et les distribue aux modules sera branchée ici (étape 2).</p>
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
        LOGGER.info("[Artemis] preInit - pont Forge -> Apollo (Lunar Client) pour 1.8.9");
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        // Couche réseau Apollo : enregistrement du canal `lunar:apollo` au login,
        // parsing des `Any` entrants et dispatch vers les modules.
        ApolloNetwork network = ApolloNetwork.getInstance();
        MinecraftForge.EVENT_BUS.register(network);

        // Module Glowing : handlers réseau + rendu de l'outline.
        GlowModule.register(network);
        MinecraftForge.EVENT_BUS.register(new GlowOverlayListener());

        // Module Nametag : handlers réseau + rendu au-dessus des joueurs.
        NametagModule.register(network);
        MinecraftForge.EVENT_BUS.register(new NametagRenderer());

        // Module Cooldown : handlers réseau + rendu HUD.
        CooldownModule.register(network);
        MinecraftForge.EVENT_BUS.register(new CooldownRenderer());

        // Module Vignette : handlers réseau + overlay plein écran.
        VignetteModule.register(network);
        MinecraftForge.EVENT_BUS.register(new VignetteRenderer());

        // Module TeamView : handlers réseau + marqueurs world-space.
        TeamModule.register(network);
        MinecraftForge.EVENT_BUS.register(new TeamRenderer());

        // Module Waypoint : handlers réseau + beam/label world-space.
        WaypointModule.register(network);
        MinecraftForge.EVENT_BUS.register(new WaypointRenderer());

        // Module Marker : handlers réseau + icône/infos conditionnelles (hover).
        MarkerModule.register(network);
        MinecraftForge.EVENT_BUS.register(new MarkerRenderer());

        // Module Colored Fire : couleur des flammes par joueur (rendu via MixinRenderFire).
        ColoredFireModule.register(network);

        // Module Limb : masquage de parties du corps par joueur (rendu via MixinModelPlayer).
        LimbModule.register(network);

        // Module Inventory (interaction) : les items tagués lunar.* sont gérés par
        // MixinGuiContainer / MixinGuiScreen — aucun handler réseau à enregistrer.

        // Chat Custom : réception sur artemis:chat (MixinNetworkManager) -> injection dans le chat
        // vanilla, rendu hex via MixinGuiNewChat. Aucun listener à enregistrer.

        // Éclairs colorés : réception sur artemis:lightning (MixinNetworkManager) -> LightningState,
        // rendu au RenderWorldLastEvent. Feature hors Apollo (comme le chat).
        MinecraftForge.EVENT_BUS.register(new LightningRenderer());

        // Commande client /artemis + ouverture différée de l'écran de config (au tick).
        ClientCommandHandler.instance.registerCommand(new ArtemisCommand());
        MinecraftForge.EVENT_BUS.register(new ArtemisGuiController());

        LOGGER.info("[Artemis] init - reseau Apollo + modules Glow/Nametag/Cooldown/Vignette/TeamView/Waypoint/Marker actifs");

        // Le module Glint (rendu par item via NBT lunar.glint) est actif via MixinRenderItem.
        // Étape suivante : Custom Chat (+ PingMarker si Apollo mis à jour).
    }

    @Mod.EventHandler
    public void postInit(FMLPostInitializationEvent event) {
        LOGGER.info("[Artemis] postInit - prêt");
    }
}
