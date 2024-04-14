package thederpgamer.starbridge.ui;

import net.dv8tion.jda.api.MessageBuilder;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * [Description]
 *
 * @author Garret Reichenbach
 */
public class ServerSettingsUI extends DiscordUI {
	protected ServerSettingsUI(Member member, TextChannel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, TextChannel channel) {
	}

	@Override
	public Message toMessage() {
		return new MessageBuilder().setActionRows(row).setContent("Server Settings Menu").build();
	}
}
