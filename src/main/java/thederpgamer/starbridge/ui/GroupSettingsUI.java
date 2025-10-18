package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

public class GroupSettingsUI extends DiscordUI {

	protected GroupSettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	protected void createUI(Member member, Channel channel) {

	}

	@Override
	public MessageCreateBuilder toMessage() {
		return null;
	}
}
