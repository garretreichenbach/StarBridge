package thederpgamer.starbridge.server.bot.commands;

import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

/**
 * DiscordCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/08/2021
 */
public interface DiscordCommand {
    void execute(MessageReceivedEvent event);
}
