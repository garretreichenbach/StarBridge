package thederpgamer.starbridge.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;

/**
 * Interface for Discord commands that can be executed via slash commands.
 */
public interface DiscordCommand {
	void execute(SlashCommandInteractionEvent event);

	CommandData getCommandData();
}
