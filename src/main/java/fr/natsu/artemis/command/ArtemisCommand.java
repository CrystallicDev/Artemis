package fr.natsu.artemis.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.gui.ArtemisGuiController;

/**
 * Commande client {@code /artemis} : ouvre l'écran de configuration d'Artemis.
 */
public final class ArtemisCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "artemis";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/artemis - ouvre l'ecran de configuration";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public boolean canCommandSenderUseCommand(ICommandSender sender) {
        return true;
    }

    @Override
    public void processCommand(ICommandSender sender, String[] args) {
        Artemis.LOGGER.info("[Config] commande /artemis recue");
        ArtemisGuiController.requestOpenConfig();
    }
}
