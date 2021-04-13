package thederpgamer.starbridge.server.commands;

import api.mod.StarLoader;
import api.utils.game.chat.CommandInterface;
import api.utils.game.chat.commands.HelpCommand;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import thederpgamer.starbridge.StarBridge;

/**
 * HelpDiscordCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/13/2021
 */
public class HelpDiscordCommand extends HelpCommand implements DiscordCommand {

    @Override
    public void execute(SlashCommandEvent event) {
        String message = event.getCommandPath().trim().replace("/", " ").toLowerCase();
        String[] split = message.split(" ");
        if(split.length == 2) {
            String command = split[1].replace("/", "");
            CommandInterface commandInterface = StarLoader.getCommand(command);
            if(commandInterface != null) {
                if(commandInterface.isAdminOnly()) {
                    if(isAdmin(event.getMember())) event.reply(command + ":\n" + commandInterface.getDescription().replace("%COMMAND%", command)).queue();
                    else event.reply("You don't have permission to view command " + command).queue();
                } else event.reply(command + ":\n" + commandInterface.getDescription().replace("%COMMAND%", command)).queue();
            } else event.reply(command + " is not a valid command.").queue();
        } else event.reply("Incorrect usage \"" + message + "\"\n Use /help help for proper usages").queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), getDescription());
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.STRING, "command", "The command's name or alias").setRequired(true));
        return commandData;
    }

    private boolean isAdmin(Member member) {
        return StarBridge.instance.getBot().hasRole(member, StarBridge.instance.adminRoleId);
    }
}
