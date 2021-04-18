package thederpgamer.starbridge.server.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import java.util.Arrays;

/**
 * StopCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/18/2021
 */
public class StopCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "stop";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Stops the server with an optional count down timer and description.\n" +
                "- %COMMAND% [seconds] [description] : Stops the server with an optional count down in seconds and a reason for the shutdown.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] args) {
        if(args.length <= 2) {
            if(args.length == 0) {
                StarBridge.instance.getBot().resetWebhook();
                StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
                GameServer.getServerState().addTimedShutdown(StarBridge.instance.defaultShutdownTimer);
                GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                return true;
            } else if(args.length == 1 & NumberUtils.isNumber(args[0].trim())) {
                int timer = Integer.parseInt(args[0].trim());
                StarBridge.instance.getBot().resetWebhook();
                StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + timer + " seconds.");
                GameServer.getServerState().addTimedShutdown(timer);
                GameServer.getServerState().addCountdownMessage(timer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                return true;
            }
        } else {
            if(NumberUtils.isNumber(args[0])) {
                StringBuilder builder = new StringBuilder();
                for(String s : Arrays.copyOfRange(args, 1, args.length)) builder.append(s).append(" ");
                String description = builder.toString().trim();
                int timer = Integer.parseInt(args[0].trim());
                StarBridge.instance.getBot().resetWebhook();
                StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + timer + " seconds.\n" + description);
                GameServer.getServerState().addTimedShutdown(timer);
                GameServer.getServerState().addCountdownMessage(timer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.\n" + description);
                return true;
            }
        }
        return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.instance;
    }

    @Override
    public void execute(SlashCommandEvent event) {
        String message = event.getCommandPath().trim().replace("/", " ").toLowerCase();
        String[] split = message.split(" ");
        boolean valid = false;
        if(split.length >= 1 && split.length < 3) {
            if(split[0].equalsIgnoreCase("stop")) {
                if(split.length == 1) {
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
                    GameServer.getServerState().addTimedShutdown(StarBridge.instance.defaultShutdownTimer);
                    GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                    valid = true;
                } else if(split[0].equalsIgnoreCase("stop") && NumberUtils.isNumber(split[1].trim())) {
                    int timer = Integer.parseInt(split[1].trim());
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + timer + " seconds.");
                    GameServer.getServerState().addTimedShutdown(timer);
                    GameServer.getServerState().addCountdownMessage(timer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                    valid = true;
                }
            }
        } else {
            if(split[0].equalsIgnoreCase("stop") && NumberUtils.isNumber(split[1])) {
                StringBuilder builder = new StringBuilder();
                for(String s : Arrays.copyOfRange(split, 2, split.length)) builder.append(s).append(" ");
                String description = builder.toString().trim();
                int timer = Integer.parseInt(split[1].trim());
                StarBridge.instance.getBot().resetWebhook();
                StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server shutting down in " + timer + " seconds.\n" + description);
                GameServer.getServerState().addTimedShutdown(timer);
                GameServer.getServerState().addCountdownMessage(timer, "Server shutting down in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.\n" + description);
                valid = true;
            }
        }
        event.getHook().deleteOriginal().queue();
        if(!valid) event.reply("Incorrect usage \"/" + message + "\". Use /help stop for proper usages.").queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), getDescription());
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.INTEGER, "countdown", "The stop countdown (in seconds)").setRequired(false));
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.STRING, "description", "The description or reason for the shutdown").setRequired(false));
        return commandData;
    }
}
