package thederpgamer.starbridge.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import thederpgamer.starbridge.ui.PanelUI;

import java.util.Objects;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class PanelCommand implements DiscordCommand {
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if(PermissionUtil.checkPermission(Objects.requireNonNull(event.getMember()), Permission.ADMINISTRATOR)) event.reply(openUI(event.getMember(), event.getChannel()).build()).setEphemeral(true).complete();
		else event.reply("You do not have permission to use this command!").setEphemeral(true).complete();
//			int seconds = Objects.requireNonNull(event.getOption("seconds")).getAsInt();
//			StarBot.getInstance().sendDiscordMessage(":warning: Server will restart in " + seconds + " seconds!");
//			GameServer.getServerState().addTimedShutdown(seconds);
	}

	@Override
	public CommandData getCommandData() {
//		CommandDataImpl commandData = new CommandDataImpl("restart", "Restarts the server.");
//		OptionData optionData = new OptionData(OptionType.INTEGER, "seconds", "Seconds until restart.", true);
//		commandData.addOptions(optionData);
//		return commandData;
		return new CommandDataImpl("panel", "Opens server control panel.");
	}

	private MessageCreateBuilder openUI(Member member, Channel channel) {
		return (new PanelUI(member, channel)).toMessage();
	}
}
