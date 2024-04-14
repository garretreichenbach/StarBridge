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
public class ConfigSettingsUI extends DiscordUI {

	protected ConfigSettingsUI(Member member, TextChannel channel) {
		super(member, channel);
	}

	@Override
	public void createUI(Member member, TextChannel channel) {
	}

	@Override
	public Message toMessage() {
		return new MessageBuilder().setActionRows(row).setContent("Config Menu").build();
	}
}
