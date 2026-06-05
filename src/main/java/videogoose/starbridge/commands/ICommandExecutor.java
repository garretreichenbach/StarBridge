package videogoose.starbridge.commands;

import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import org.schema.game.common.data.player.PlayerState;

/**
 * Interface implemented by command handlers to run a StarBridge command from both the in-game chat and a Discord context.
 *
 * @author VideoGoose
 */
public interface ICommandExecutor {

	boolean executeGame(PlayerState sender, String[] args);

	void executeDiscord(SlashCommandInteractionEvent event);

	CommandData getDiscordCommandData();
}
