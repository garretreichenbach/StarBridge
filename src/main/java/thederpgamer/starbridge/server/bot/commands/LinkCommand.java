package thederpgamer.starbridge.server.bot.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.utils.LogUtils;
import thederpgamer.starbridge.utils.MessageType;

/**
 * LinkCommand
 * <Description>
 *
 * @author Garret Reichenbach
 * @since 04/09/2021
 */
public class LinkCommand implements DiscordCommand, CommandInterface {

    private final String[] permissions = {
            "*",
            "chat.*",
            "chat.command.*",
            "chat.command.link"
    };

    @Override
    public String getCommand() {
        return "link";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "link%SEPARATOR%discord",
                "discord%SEPARATOR%link"
        };
    }

    @Override
    public String getDescription() {
        return "Gives you a special code that can be used to link your discord account to your StarMade account on SOE.\n" +
                "- %COMMAND% : PMs you a link code. Use /link <code> in the #bot-commands discord channel to link your accounts.";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] strings) {
        StarBridge.instance.botThread.getBot().addLinkRequest(playerState);
        return true;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return null;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        String[] split = event.getMessage().getContentDisplay().replace("/", "").split(" ");
        if(split.length == 2) {
            try {
                PlayerData playerData = StarBridge.instance.botThread.getBot().getLinkRequest(Integer.parseInt(split[1]));
                if(playerData != null) {
                    playerData.setDiscordId(event.getAuthor().getIdLong());
                    ServerDatabase.updatePlayerData(playerData);
                    PersistentObjectUtil.save(StarBridge.instance.getSkeleton());
                    String logMessage = "Successfully linked user " + event.getAuthor().getName() + " to " + playerData.getPlayerName() + ".";
                    StarBridge.instance.botThread.getBot().removeLinkRequest(playerData);
                    StarBridge.instance.botThread.getBot().sendTimedMessageFromBot(logMessage, 15);
                    LogUtils.logMessage(MessageType.INFO, logMessage);
                    event.getMessage().delete().queue();
                    return;
                }
            } catch(Exception e) {
                e.printStackTrace();
            }
        }
        StarBridge.instance.botThread.getBot().sendTimedMessageFromBot("Sorry " + event.getAuthor().getName() + ", but that link code is invalid.", 15);
    }
}
