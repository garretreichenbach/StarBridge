package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;

public class SettingsUI extends DiscordUI {

	public SettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, Channel channel) {
		Button userSettingsButton = new ButtonImpl("user_settings", "User Settings", ButtonStyle.PRIMARY, false, null);
		Button groupSettingsButton = new ButtonImpl("group_settings", "Group Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button botSettingsButton = new ButtonImpl("bot_settings", "Bot Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		Button serverSettingsButton = new ButtonImpl("server_settings", "Server Settings", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), null);
		addComponent(userSettingsButton, interaction -> interaction.reply((new UserSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue());
		addComponent(groupSettingsButton, interaction -> interaction.reply((new GroupSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue());
		addComponent(botSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
				return;
			}
			interaction.reply((new BotSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue();
		});
		addComponent(serverSettingsButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
				return;
			}
			interaction.reply((new ServerSettingsUI(member, channel)).toMessage().build()).setEphemeral(true).queue();
		});
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getComponents()).setContent("Settings Menu");
	}
}
