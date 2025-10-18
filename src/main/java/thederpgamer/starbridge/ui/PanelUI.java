package thederpgamer.starbridge.ui;

import api.common.GameServer;
import com.vdurmont.emoji.EmojiParser;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.interactions.components.buttons.ButtonStyle;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.internal.interactions.component.ButtonImpl;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import thederpgamer.starbridge.bot.MessageType;


public class PanelUI extends DiscordUI {

	public PanelUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, Channel channel) {
		Button restartButton = new ButtonImpl("restart_button", "Restart Server (5 min timer)", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), Emoji.fromUnicode(EmojiParser.parseToUnicode(":arrows_counterclockwise:")));
		Button restartNowButton = new ButtonImpl("restart_now_button", "Restart Server (immediate)", ButtonStyle.PRIMARY, !PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR), Emoji.fromUnicode(EmojiParser.parseToUnicode(":arrows_counterclockwise:")));

		addComponent(restartButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			} else {
				GameServer.getServerState().addTimedShutdown(300);
				MessageType.SERVER_RESTARTING_TIMED.sendMessage(300);
				interaction.reply("Server will restart in 5 minutes.").queue();
			}
		});
		addComponent(restartNowButton, interaction -> {
			if(!PermissionUtil.checkPermission(member, Permission.ADMINISTRATOR)) {
				interaction.reply("You do not have permission to access this menu.").setEphemeral(true).queue();
			} else {
				GameServer.getServerState().addTimedShutdown(10);
				MessageType.SERVER_RESTARTING.sendMessage();
				interaction.reply("Server will restart in 10 seconds.").queue();
			}
		});
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getActionComponents());
	}
}
