package fr.natsu.artemis.command;

import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import fr.natsu.artemis.Artemis;
import fr.natsu.artemis.gui.ArtemisGuiController;

/**
 * Client command {@code /artemis}: opens the Artemis configuration screen.
 */
public final class ArtemisCommand extends CommandBase {

    @Override
    public String getCommandName() {
        return "artemis";
    }

    @Override
    public String getCommandUsage(ICommandSender sender) {
        return "/artemis - opens the configuration screen";
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
        Artemis.LOGGER.info("[Config] /artemis command received");
        ArtemisGuiController.requestOpenConfig();
    }
}
