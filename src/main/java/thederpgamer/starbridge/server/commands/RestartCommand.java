package thederpgamer.starbridge.server.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.StarRunnable;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.schema.common.util.StringTools;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.utils.ServerUtils;

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
            StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds\n");
            GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds\n");
            triggerRestart(StarBridge.instance.defaultShutdownTimer);
            return true;
        } else {
            if(args.length == 1) {
                if(NumberUtils.isNumber(args[0]) && Integer.parseInt(args[0]) >= 1) {
                    int timer = Integer.parseInt(args[0]);
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds\n");
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + timer + " seconds\n");
                    triggerRestart(timer);
                    return true;
                } else return false;
            } else if(args.length == 2) {
                if(NumberUtils.isNumber(args[0]) && Integer.parseInt(args[0]) >= 1) {
                    String description = args[1];
                    int timer = Integer.parseInt(args[0]);
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds: " + description + "\n");
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + timer + " seconds: " + description + "\n");
                    triggerRestart(timer);
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
        String[] args = StringTools.splitParameters(message);
        if(args.length == 1) {
            StarBridge.instance.getBot().resetWebhook();
            StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
            GameServer.getServerState().addCountdownMessage(StarBridge.instance.defaultShutdownTimer, "Server restarting in " + StarBridge.instance.defaultShutdownTimer + " seconds.");
            triggerRestart(StarBridge.instance.defaultShutdownTimer);
            event.acknowledge(true).queue();
        } else {
            if(event.getOption("countdown") != null && NumberUtils.isNumber(event.getOption("countdown").getAsString())) {
                int timer = Integer.parseInt(Objects.requireNonNull(event.getOption("countdown")).getAsString());
                if(args.length == 2) {
                    String description = args[1];
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds: " + description + "\n");
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + timer + " seconds: " + description + "\n");
                } else {
                    StarBridge.instance.getBot().resetWebhook();
                    StarBridge.instance.getBot().sendMessageToDiscord(":octagonal_sign: Server restarting in " + timer + " seconds\n");
                    GameServer.getServerState().addCountdownMessage(timer, "Server restarting in " + timer + " seconds\n");
                }
                triggerRestart(timer);
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

    private void triggerRestart(int timer) {
        new StarRunnable() {
            @Override
            public void run() {
                StarBridge.instance.getBot().sendServerRestartMessage();
                ServerUtils.triggerRestart();
            }
        }.runLater(StarBridge.instance, timer * 1000L);
    }
}