package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import thederpgamer.starbridge.utils.PlayerUtils;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class UserSettingsUI extends DiscordUI {
	protected UserSettingsUI(Member member, TextChannel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, TextChannel channel) {
		if(isLinked(member)) {
//			Button button = new ButtonImpl();
		} else {

		}
	}

	@Override
	public Message toMessage() {
		return new MessageBuilder().setActionRows(row).setContent("User Settings Menu").build();
	}

	private boolean isLinked(Member member) {
		return PlayerUtils.getPlayerDataFromDiscordID(Long.parseLong(member.getId())) != null;
	}
}
