package thederpgamer.starbridge.server.commands;

import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;

/**
 * DiscordCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/08/2021
 */
public interface DiscordCommand {
    void execute(SlashCommandEvent event);
    CommandUpdateAction.CommandData getCommandData();
}
