package thederpgamer.starbridge.server.commands;

import api.common.GameCommon;
import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;

/**
 * InfoPlayerCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/27/2021
 */
public class InfoPlayerCommand implements CommandInterface, DiscordCommand {
    @Override
    public String getCommand() {
        return "info_p";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "info%SEPARATOR%p",
                "info%SEPARATOR%player",
                "info",
                "who%SEPARATOR%p",
                "who%SEPARATOR%player",
                "who%SEPARATOR%is",
                "who"
        };
    }

    @Override
    public String getDescription() {
        return "Displays information about a player.\n" +
                "- /%COMMAND% <player_name>";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args.length == 1) {
            PlayerData playerData = ServerDatabase.getPlayerData(args[0]);
            if(playerData != null) PlayerUtils.sendMessage(sender, formatPlayerData(playerData));
            else PlayerUtils.sendMessage(sender, "Player " + args[0] + " doesn't exist!");
            return true;
        } else return false;
    }

    @Override
    public void serverAction(@Nullable PlayerState sender, String[] args) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.instance;
    }

    @Override
    public void execute(SlashCommandEvent event) {
        String message = event.getCommandPath().trim().replace("/", " ").toLowerCase();
        String[] split = message.split(" ");
        if(split.length == 2) {
            PlayerData playerData = ServerDatabase.getPlayerData(split[1]);
            if(playerData != null) event.reply(formatPlayerData(playerData)).queue();
            else event.reply("Player " + split[1] + "doesn't exist!").queue();
        } else event.reply("Incorrect usage \"/" + message + "\". Use /help info_p for proper usages.").queue();
        event.getHook().deleteOriginal().queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), "Displays information about a player.");
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.STRING, "player_name", "The name of the player").setRequired(true));
        return commandData;
    }

    private String formatPlayerData(PlayerData playerData) {
        StringBuilder builder = new StringBuilder();
        builder.append("Info for ").append(playerData.getPlayerName()).append(":\n");
        builder.append("Faction: ").append(playerData.getFactionName()).append("\n");
        builder.append("Time played: ").append(playerData.getHoursPlayed()).append(" hours\n");
        if(playerData.getDiscordId() != -1) {
            try {
                String tag = StarBridge.instance.getBot().bot.retrieveUserById(playerData.getDiscordId()).complete().getAsTag();
                builder.append("Discord: ").append(tag).append("\n");
            } catch(Exception ignored) { }
        }
        builder.append("Status: ");
        if(GameCommon.getPlayerFromName(playerData.getPlayerName()) != null) builder.append("ONLINE");
        else builder.append("OFFLINE");
        return builder.toString().trim();
    }
}
