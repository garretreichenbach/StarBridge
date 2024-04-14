package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class BotSettingsUI extends DiscordUI {

	protected BotSettingsUI(Member member, TextChannel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, TextChannel channel) {
		Button exceptionLoggingSettingsButton = new ButtonImpl("exception_logging_settings", "Exception Logging Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button configSettingsButton = new ButtonImpl("config_settings", "Config Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		addComponent(exceptionLoggingSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			else interaction.reply((new ExceptionSettingsUI(member, channel)).toMessage()).setEphemeral(true).queue();
		});
		addComponent(configSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			else interaction.reply((new ConfigSettingsUI(member, channel)).toMessage()).setEphemeral(true).queue();
		});
	}

	@Override
	public Message toMessage() {
		return new MessageBuilder().setActionRows(row).setContent("Bot Settings Menu").build();
	}
}
