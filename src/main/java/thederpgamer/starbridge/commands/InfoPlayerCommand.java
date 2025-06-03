package thederpgamer.starbridge.commands;

import api.common.GameCommon;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;

/**
 * InfoPlayerCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/27/2021
 */
public class InfoPlayerCommand implements CommandInterface, DiscordCommand {
	@Override
	public String getCommand() {
		return "info_p";
	}

	@Override
	public String[] getAliases() {
		return new String[] {"info%SEPARATOR%p", "info%SEPARATOR%player", "info", "who%SEPARATOR%p", "who%SEPARATOR%player", "who%SEPARATOR%is", "who"};
	}

	@Override
	public String getDescription() {
		return "Displays information about a player.\n" + "- /%COMMAND% <player_name>";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState sender, String[] args) {
		if(args.length == 1) {
			PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(args[0]);
			if(playerData != null) PlayerUtils.sendMessage(sender, formatPlayerData(playerData));
			else PlayerUtils.sendMessage(sender, "Player " + args[0] + " doesn't exist!");
			return true;
		} else return false;
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
		String playerName = event.getOption("player_name").getAsString();
		PlayerData playerData = ServerDatabase.getPlayerDataOrCreateIfNull(playerName);
		event.reply(formatPlayerData(playerData)).queue();
	}

	@Override
	public CommandData getCommandData() {
		CommandDataImpl commandData = new CommandDataImpl(getCommand(), "Displays information about a player.");
		commandData.addOption(OptionType.STRING, "player_name", "The name of the player", true);
		return commandData;
	}

	private String formatPlayerData(PlayerData playerData) {
		StringBuilder builder = new StringBuilder();
		builder.append("Info for ").append(playerData.getPlayerName()).append(":\n");
		builder.append("Faction: ").append(playerData.getFactionName()).append("\n");
		builder.append("Time played: ").append(String.format("%,.2f", (playerData.getHoursPlayed()))).append(" hours\n");
		if(playerData.getDiscordId() != -1) {
			try {
				String tag = StarBridge.getBot().getJDA().retrieveUserById(playerData.getDiscordId()).complete().getAsTag();
				builder.append("Discord: ").append(tag).append("\n");
			} catch(Exception ignored) {
			}
		}
		builder.append("Status: ");
		if(GameCommon.getPlayerFromName(playerData.getPlayerName()) != null) builder.append("ONLINE");
		else builder.append("OFFLINE");
		return builder.toString().trim();
	}
}
