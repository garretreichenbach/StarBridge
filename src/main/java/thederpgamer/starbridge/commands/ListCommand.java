package thederpgamer.starbridge.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
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
		return new String[] {"li"};
	}

	@Override
	public String getDescription() {
		return "Lists the specified server info.\n" + "- /%COMMAND% players : Displays the list of players currently online.\n" + "- /%COMMAND% staff : Displays the list of staff currently online.";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		String command = null;
		if(args == null || args.length == 0) command = "list players";
		else if("players".equalsIgnoreCase(args[0]) || "p".equalsIgnoreCase(args[0])) command = "list players";
		else if("staff".equalsIgnoreCase(args[0]) || "s".equalsIgnoreCase(args[0])) command = "list staff";
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
		return StarBridge.getInstance();
	}

	@Override
	public void execute(SlashCommandInteractionEvent event) {
		String message = event.getCommandString().trim().replace("/", " ").toLowerCase();
		String type = "players";
		if(event.getOption("list") != null) type = event.getOption("list").getAsString();
		StringBuilder builder = new StringBuilder();
		if("players".equalsIgnoreCase(type) || "p".equalsIgnoreCase(type)) {
			try {
				builder.append("Current Online Players:\n");
				for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
					builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
				}
			} catch(NullPointerException exception) {
				StarBridge.getInstance().logWarning("Encountered a NullPointerException while trying to fetch list of online players! This is most likely due to there being no players currently online.");
				builder = new StringBuilder();
				builder.append("There are no players currently online.");
			}
			event.reply(builder.toString().trim()).queue();
		} else if("staff".equalsIgnoreCase(type) || "s".equalsIgnoreCase(type)) {
			try {
				builder.append("Current Online Staff:\n");
				for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
					if(playerState.isAdmin()) builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
				}
			} catch(NullPointerException exception) {
				StarBridge.getInstance().logWarning("Encountered a NullPointerException while trying to fetch list of online staff! This is most likely due to there being no staff currently online.");
				builder = new StringBuilder();
				builder.append("There are no staff currently online.");
			}
			event.reply(builder.toString().trim()).queue();
		} else event.reply("Incorrect usage \"/" + message + "\". Use /help list for proper usages.").queue();
	}

	@Override
	public CommandData getCommandData() {
		CommandDataImpl commandData = new CommandDataImpl("list", "Lists the specified server info");
		OptionData optionData = new OptionData(OptionType.STRING, "list", "The data to list");
		optionData.addChoice("players", "players");
		optionData.addChoice("staff", "staff");
		commandData.addOptions(optionData);
		return commandData;
	}
}
