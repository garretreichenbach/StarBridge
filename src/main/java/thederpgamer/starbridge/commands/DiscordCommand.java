package thederpgamer.starbridge.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * DiscordCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/08/2021
 */
public interface DiscordCommand {
	void execute(SlashCommandInteractionEvent event);

	CommandData getCommandData();
}
