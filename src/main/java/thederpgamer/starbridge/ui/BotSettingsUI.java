package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class BotSettingsUI extends DiscordUI {

	protected BotSettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, Channel channel) {
		Button exceptionLoggingSettingsButton = new ButtonImpl("exception_logging_settings", "Exception Logging Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button configSettingsButton = new ButtonImpl("config_settings", "Config Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		addComponent(exceptionLoggingSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			} else {
				interaction.reply((new ExceptionSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue();
			}
		});
		addComponent(configSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			} else {
				interaction.reply((new ConfigSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue();
			}
		});
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getActionComponents()).setContent("Bot Settings Menu");
	}
}
