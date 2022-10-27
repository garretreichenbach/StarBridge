package thederpgamer.starbridge.commands;

import api.common.GameServer;
import api.mod.StarLoader;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.interactions.commands.build.OptionData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.starbridge.bot.StarBot;
import thederpgamer.starbridge.server.ServerDatabase;

import java.util.Objects;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class RestartCommand implements DiscordCommand {
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		try {
			if(!PermissionUtil.checkPermission(Objects.requireNonNull(event.getMember()), Permission.ADMINISTRATOR)) {
				event.reply("You do not have permission to use this command!").setEphemeral(true).queue();
				return;
			}
			int seconds = Objects.requireNonNull(event.getOption("seconds")).getAsInt();
			StarBot.getInstance().sendDiscordMessage(":warning: Server will restart in " + seconds + " seconds!");
			GameServer.getServerState().addTimedShutdown(seconds);
		} catch(Exception exception) {
			event.reply("Invalid seconds value!").setEphemeral(true).queue();
		}
	}

	@Override
	public CommandData getCommandData() {
		CommandDataImpl commandData = new CommandDataImpl("restart", "Restarts the server.");
		OptionData optionData = new OptionData(OptionType.INTEGER, "seconds", "Seconds until restart.", true);
		commandData.addOptions(optionData);
		return commandData;
	}
}
