package thederpgamer.starbridge.server.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;

import javax.annotation.Nullable;

/**
 * ListCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/08/2021
 */
public class ListCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "list";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "li"
        };
    }

    @Override
    public String getDescription() {
        return "Lists the specified server info.\n" +
                "- /%COMMAND% players : Displays the list of players currently online.\n" +
                "- /%COMMAND% staff : Displays the list of staff currently online.";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        String command = null;
        if(args == null || args.length == 0) command = "list players";
        else if(args[0].equalsIgnoreCase("players") || args[0].equalsIgnoreCase("p")) command = "list players";
        else if(args[0].equalsIgnoreCase("staff") || args[0].equalsIgnoreCase("s")) command = "list staff";
        if(command != null) {
            switch(command) {
                case "list players":
                    StringBuilder playerBuilder = new StringBuilder();
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        playerBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
                    PlayerUtils.sendMessage(sender, "Current Online Players:\n" + playerBuilder.toString().trim());
                    return true;
                case "list staff":
                    StringBuilder staffBuilder = new StringBuilder();
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        if(playerState.isAdmin()) staffBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
                    PlayerUtils.sendMessage(sender, "Current Online Staff:\n" + staffBuilder.toString().trim());
                    return true;
            }
        }
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.instance;
    }

    @Override
    public void execute(SlashCommandEvent event) {
        String message = event.getCommandPath().trim().replace("/", " ").toLowerCase();
        String type = event.getOption("list").getAsString();
        StringBuilder builder = new StringBuilder();
        if(type.equalsIgnoreCase("players") || type.equalsIgnoreCase("p")) {
            builder.append("Current Online Players:\n");
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
            }
            event.reply(builder.toString().trim()).queue();
        } else if(type.equalsIgnoreCase("staff") || type.equalsIgnoreCase("s")) {
            builder.append("Current Online Staff:\n");
            for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                if(playerState.isAdmin()) builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
            }
            event.reply(builder.toString().trim()).queue();
        } else {
            event.reply("Incorrect usage \"/" + message + "\". Use /help list for proper usages.").queue();
        }
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData("list", "Lists the specified server info");
        CommandUpdateAction.OptionData optionData = new CommandUpdateAction.OptionData(Command.OptionType.STRING, "list", "The data to list");
        optionData.addChoice("players", "players");
        optionData.addChoice("staff", "staff");
        commandData.addOption(optionData);
        return commandData;
    }
}
