package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import thederpgamer.starbridge.server.ServerDatabase;


public class UserSettingsUI extends DiscordUI {

	protected UserSettingsUI(Member member, Channel channel) {
		super(member, channel);
	}

	@Override
	protected void createUI(Member member, Channel channel) {
		if(isLinked(member)) {
//			Button button = new ButtonImpl();
		} else {

		}
	}

	@Override
	public MessageCreateBuilder toMessage() {
		return new MessageCreateBuilder().setActionRow(row.getActionComponents()).setContent("User Settings Menu");
	}

	private boolean isLinked(Member member) {
		return ServerDatabase.getFromDiscordID(member.getIdLong()) != null;
	}
}
