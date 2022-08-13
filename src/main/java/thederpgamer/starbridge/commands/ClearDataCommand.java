package thederpgamer.starbridge.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.apache.commons.lang3.math.NumberUtils;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.LogManager;

import java.util.Objects;

/**
 * Clears old mod data for StarBridge.
 *
 * @version 2.0 - [09/11/2021]
 * @author TheDerpGamer
 */
public class ClearDataCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "clear_data";
    }

    @Override
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getDescription() {
        return "Clears old (inactive) data of the specified type. If an amount is specified, attempts to clear only that amount of files starting with the oldest entries.\n" +
               "- /%COMMAND% logs [amount] : Clears the specified amount of inactive logs, starting the oldest ones first.\n" +
               "- /%COMMAND% chat [amount] : Clears the specified amount of inactive chat logs, starting with the oldest ones first.\n" +
               "- /%COMMAND% player_data <player_name> : Clears any PlayerData saved by StarBridge for the specified player.";
    }

    @Override
    public boolean isAdminOnly() {
        return true;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args.length >= 1) {
            String subCommand = args[0].toLowerCase();
            switch(subCommand) {
                case "logs":
                    if(args.length == 1) PlayerUtils.sendMessage(sender, "Cleared " + LogManager.clearLogs() + " logs."); //No amount is specified, so clear all inactive logs
                    else if(args.length == 2 && NumberUtils.isDigits(args[1])) PlayerUtils.sendMessage(sender, "Cleared " + LogManager.clearLogs(Integer.parseInt(args[1])) + " logs.");
                    else return false;
                    return true;
                case "chat":
                    if(args.length == 1) PlayerUtils.sendMessage(sender, "Cleared " + LogManager.clearChat() + " chat logs."); //No amount is specified, so clear all inactive chat logs
                    else if(args.length == 2 && NumberUtils.isDigits(args[1])) PlayerUtils.sendMessage(sender, "Cleared " + LogManager.clearChat(Integer.parseInt(args[1])) + " chat logs.");
                    else return false;
                    return true;
                case "player_data":
                    return true;
            }

        } else return false;
        /*
        if(args == null || args.length == 0 || (args.length == 1 && (args[0].equalsIgnoreCase("logs")))) {
            LogManager.clearLogs();
            PlayerUtils.sendMessage(playerState, "Successfully cleared " + (StarBridge.instance.maxWorldLogs - 1) + " logs");
            return true;
        } else return false;
         */
        return true;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.getInstance();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        String message = event.getCommandPath().trim().replace("/", " ").toLowerCase();
        String dataType = Objects.requireNonNull(event.getOption("data_type")).getAsString();
        if(dataType.equalsIgnoreCase("logs")) {
            LogManager.clearLogs();
            event.reply("Successfully cleared " + (ConfigManager.getMainConfig().getInt("max-world-logs") - 1) + " logs").queue();
        } else event.reply("Incorrect usage \"/" + message + "\". Use /help clear for proper usages.").queue();
    }

    @Override
    public CommandData getCommandData() {
        CommandDataImpl commandData = new CommandDataImpl(getCommand(), "Clears old data of the specified type");
        commandData.addOption(OptionType.STRING, "data_type", "The type of the data to clear", true);
        return commandData;
    }
}
