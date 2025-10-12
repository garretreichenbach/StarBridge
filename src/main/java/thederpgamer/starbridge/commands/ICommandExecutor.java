package thederpgamer.starbridge.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.schema.game.common.data.player.PlayerState;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public interface ICommandExecutor {

	boolean executeGame(PlayerState sender, String[] args);

	void executeDiscord(SlashCommandInteractionEvent event);

	CommandData getDiscordCommandData();
}
