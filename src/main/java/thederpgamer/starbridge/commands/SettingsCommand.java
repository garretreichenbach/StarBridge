package thederpgamer.starbridge.commands;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import thederpgamer.starbridge.ui.SettingsUI;

import java.util.Objects;

/**
 * Command for opening the settings UI.
 *
 * @author Garret Reichenbach
 */
public class SettingsCommand implements DiscordCommand {
	@Override
	public void execute(SlashCommandInteractionEvent event) {
		if(PermissionUtil.checkPermission(Objects.requireNonNull(event.getMember()), Permission.ADMINISTRATOR)) event.reply(openUI(event.getMember(), event.getChannel()).build()).setEphemeral(true).queue();
		else event.reply("You do not have permission to use this command!").setEphemeral(true).queue();
	}

	@Override
	public CommandData getCommandData() {
		return new CommandDataImpl("settings", "Displays Settings");
	}

	private MessageCreateBuilder openUI(Member member, Channel channel) {
		return (new SettingsUI(member, channel)).toMessage();
	}
}
