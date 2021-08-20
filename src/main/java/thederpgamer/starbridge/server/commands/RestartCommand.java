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
import java.util.Objects;

/**
 * RestartCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/18/2021
 */
public class RestartCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "restart";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Restarts the server with an optional count down timer and description.\n" +
                "- /%COMMAND% [seconds] [description] : Restarts the server with an optional count down in seconds and a reason for the shutdown.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] args) {
        if(args == null || args.length == 0) {
            StarBridge.instance.getBot().resetWebhook();
            StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
            GameServer.getServerState().addTimedShutdown(StarBridge.instance.defaultShutdownTimer);
            GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
            return true;
        } else {
            if(args.length == 1) {
                if(NumberUtils.isNumber(args[0]) && Integer.parseInt(args[0]) >= 1) {
                    int timer = Integer.parseInt(args[0]);
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds.");
                    GameServer.getServerState().addTimedShutdown(timer);
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                    return true;
                } else return false;
            } else if(args.length == 2) {
                if(NumberUtils.isNumber(args[0]) && Integer.parseInt(args[0]) >= 1) {
                    StringBuilder builder = new StringBuilder();
                    for(String s : Arrays.copyOfRange(args, 1, args.length)) builder.append(s);
                    String description = builder.toString().replace("\"", "").trim();
                    int timer = Integer.parseInt(args[0]);
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds.\n" + description);
                    GameServer.getServerState().addTimedShutdown(timer);
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.\n" + description);
                    return true;
                }
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
        if(split.length == 1) {
            StarBridge.instance.getBot().resetWebhook();
            StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
            GameServer.getServerState().addTimedShutdown(StarBridge.instance.defaultShutdownTimer);
            GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
            event.acknowledge(true).queue();
        } else {
            if(event.getOption("countdown") != null && NumberUtils.isNumber(event.getOption("countdown").getAsString())) {
                int timer = Integer.parseInt(Objects.requireNonNull(event.getOption("countdown")).getAsString());
                if(split.length > 2) {
                    StringBuilder builder = new StringBuilder();
                    for(String s : Arrays.copyOfRange(split, 2, split.length)) builder.append(s).append(" ");
                    String description = builder.toString().trim().replace("\"", "");
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds.\n" + description);
                    GameServer.getServerState().addTimedShutdown(timer);
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.\n" + description);
                } else {
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds.");
                    GameServer.getServerState().addTimedShutdown(timer);
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + GameServer.getServerState().getTimedShutdownSeconds() + " seconds.");
                }
                event.acknowledge(true).queue();
            }
        }
        if(!event.isAcknowledged()) event.reply("Incorrect usage \"/" + message + "\". Use /help restart for proper usages.").queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), "Restarts the server with an optional count down timer and description.");
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.INTEGER, "countdown", "The countdown (in seconds)").setRequired(false));
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.STRING, "description", "The reason for the restart").setRequired(false));
        return commandData;
    }
}