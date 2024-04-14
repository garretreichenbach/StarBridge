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
public class SettingsUI extends DiscordUI {

	public SettingsUI(Member member, TextChannel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, TextChannel channel) {
		Button userSettingsButton = new ButtonImpl("user_settings", "User Settings", ButtonStyle.PRIMARY, false, null);
		Button botSettingsButton = new ButtonImpl("bot_settings", "Bot Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button serverSettingsButton = new ButtonImpl("server_settings", "Server Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		addComponent(userSettingsButton, interaction -> interaction.reply((new UserSettingsUI(member, channel)).toMessage()).setEphemeral(true).queue());
		addComponent(botSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
				return;
			}
			interaction.reply((new BotSettingsUI(member, channel)).toMessage()).setEphemeral(true).queue();
		});
		addComponent(serverSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
				return;
			}
			interaction.reply((new ServerSettingsUI(member, channel)).toMessage()).setEphemeral(true).queue();
		});
	}

	@Override
	public Message toMessage() {
		return new MessageBuilder().setActionRows(row).setContent("Settings Menu").build();
	}
}
