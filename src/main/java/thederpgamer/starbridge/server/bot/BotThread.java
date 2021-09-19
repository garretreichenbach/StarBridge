package thederpgamer.starbridge.server.bot;

import api.mod.config.FileConfiguration;

/**
 * Handles the main thread for the discord bot.
 *
 * @version 1.0 - [04/09/2021]
 * @author TheDerpGamer
 */
public class BotThread extends Thread {

    private final DiscordBot discordBot;

    public BotThread(FileConfiguration config) {
        String token = config.getString("bot-token");
        String chatWebhook = "https://" + config.getString("chat-webhook");
        long chatChannelId = config.getLong("chat-channel-id");
        String logWebhook = "https://" + config.getString("log-webhook");
        long logChannelId = config.getLong("log-channel-id");
        discordBot = new DiscordBot(token, chatWebhook, chatChannelId, logWebhook, logChannelId);
    }

    @Override
    public void run() {
        discordBot.initialize();
    }

    public DiscordBot getBot() {
        return discordBot;
    }
}
