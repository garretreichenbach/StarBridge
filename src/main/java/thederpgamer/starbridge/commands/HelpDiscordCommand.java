package thederpgamer.starbridge.commands;

import api.mod.StarLoader;
import api.utils.game.chat.CommandInterface;
import api.utils.game.chat.commands.HelpCommand;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.manager.ConfigManager;

/**
 * HelpDiscordCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/13/2021
 */
public class HelpDiscordCommand extends HelpCommand implements DiscordCommand {

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String command = event.getOption("command").getAsString();
        CommandInterface commandInterface = StarLoader.getCommand(command);
        if(commandInterface != null) {
            if(commandInterface.isAdminOnly()) {
                if(isAdmin(event.getMember())) event.reply(command + ":\n" + commandInterface.getDescription().replace("%COMMAND%", command)).complete();
                else event.reply("You don't have permission to view command " + command).complete();
            } else event.reply(command + ":\n" + commandInterface.getDescription().replace("%COMMAND%", command)).complete();
        } else event.reply(command + " is not a valid command.").complete();
    }

    @Override
    public CommandData getCommandData() {
        CommandDataImpl commandData = new CommandDataImpl(getCommand(), "Displays description and usages for a specified command");
        commandData.addOption(OptionType.STRING, "command", "The command's name or alias", true);
        return commandData;
    }

    private boolean isAdmin(Member member) {
        return StarBridge.getBot().hasRole(member, ConfigManager.getMainConfig().getLong("admin-role-id"));
    }
}
