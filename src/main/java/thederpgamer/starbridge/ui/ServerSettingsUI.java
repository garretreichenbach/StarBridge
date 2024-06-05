package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ServerSettingsUI extends DiscordUI {
	protected ServerSettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, Channel channel) {
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getActionComponents()).setContent("Server Settings Menu");
	}
}
