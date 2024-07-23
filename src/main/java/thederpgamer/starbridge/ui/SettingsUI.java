package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class SettingsUI extends DiscordUI {

	public SettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, Channel channel) {
		Button userSettingsButton = new ButtonImpl("user_settings", "User Settings", ButtonStyle.PRIMARY, false, null);
		Button botSettingsButton = new ButtonImpl("bot_settings", "Bot Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button serverSettingsButton = new ButtonImpl("server_settings", "Server Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		addComponent(userSettingsButton, interaction -> interaction.reply((new UserSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).complete());
		addComponent(botSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).complete();
				return;
			}
			interaction.reply((new BotSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).complete();
		});
		addComponent(serverSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).complete();
				return;
			}
			interaction.reply((new ServerSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).complete();
		});
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getComponents()).setContent("Settings Menu");
	}
}
