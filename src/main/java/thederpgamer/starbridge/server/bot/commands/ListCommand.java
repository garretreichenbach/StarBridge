package thederpgamer.starbridge.server.bot.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;

import javax.annotation.Nullable;
import java.util.Arrays;

/**
 * ListCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/08/2021
 */
public class ListCommand implements CommandInterface, DiscordCommand {

    private final String[] permissions = {
            "*",
            "chat.*",
            "chat.command.*",
            "chat.command.list"
    };

    @Override
    public String getCommand() {
        return "list";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "li"
        };
    }

    @Override
    public String getDescription() {
        return "Lists the specified server info.\n" +
                "- %COMMAND% players : Displays the list of players currently online.\n" +
                "- %COMMAND% staff : Displays the list of staff currently online.";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        String command = null;
        if(args == null || args.length == 0) command = "list players";
        else if(args[0].equalsIgnoreCase("players") || args[0].equalsIgnoreCase("p")) command = "list players";
        else if(args[0].equalsIgnoreCase("staff") || args[0].equalsIgnoreCase("s")) command = "list staff";
        if(command != null) {
            PlayerData playerData = ServerDatabase.getPlayerData(sender.getName());
            switch(command) {
                case "list players":
                    StringBuilder playerBuilder = new StringBuilder();
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        playerBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
                    PlayerUtils.sendMessage(sender, "Current Online Players:\n" + playerBuilder.toString().trim());
                    return true;
                case "list staff":
                    StringBuilder staffBuilder = new StringBuilder();
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        if(playerState.isAdmin()) staffBuilder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
                    PlayerUtils.sendMessage(sender, "Current Online Staff:\n" + staffBuilder.toString().trim());
                    return true;
            }
        }
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.instance;
    }

    @Override
    public void execute(MessageReceivedEvent event) {
        String message = event.getMessage().getContentDisplay();
        String command = null;
        String[] split = message.split(" ");
        String[] args = Arrays.copyOfRange(split, 1, split.length);
        if(args.length == 0) command = "list players";
        else if(args[0].equalsIgnoreCase("players") || args[0].equalsIgnoreCase("p")) command = "list players";
        else if(args[0].equalsIgnoreCase("staff") || args[0].equalsIgnoreCase("s")) command = "list staff";
        if(command != null) {
            StringBuilder builder = new StringBuilder();
            switch(command) {
                case "list players":
                    builder.append("Current Online Players:\n");
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
                case "list staff":
                    builder.append("Current Online Staff:\n");
                    for(PlayerState playerState : GameServer.getServerState().getPlayerStatesByName().values()) {
                        if(playerState.isAdmin()) builder.append(playerState.getName()).append(" [").append(playerState.getFactionName()).append("]\n");
                    }
            }
            StarBridge.instance.botThread.getBot().sendTimedMessageFromBot(builder.toString().trim(), 15);
        }
    }
}
