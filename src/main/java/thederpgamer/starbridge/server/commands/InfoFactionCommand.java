package thederpgamer.starbridge.server.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.server.ServerDatabase;

/**
 * InfoFactionCommand
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/27/2021
 */
public class InfoFactionCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "info_f";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "info%SEPARATOR%f",
                "info%SEPARATOR%faction",
                "who%SEPARATOR%f",
                "who%SEPARATOR%faction",
        };
    }

    @Override
    public String getDescription() {
        return "Displays information about a faction.\n" +
                "- /%COMMAND% <faction_name>";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState sender, String[] args) {
        if(args.length == 1) {
            Faction faction = ServerDatabase.getFaction(args[0]);
            if(faction != null) PlayerUtils.sendMessage(sender, formatFactionData(faction));
            else PlayerUtils.sendMessage(sender, "Faction " + args[0] + " doesn't exist!");
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
            Faction faction = ServerDatabase.getFaction(split[1]);
            if(faction != null) event.reply(formatFactionData(faction)).queue();
            else event.reply("Faction " + split[1] + "doesn't exist!").queue();
        } else event.reply("Incorrect usage \"/" + message + "\". Use /help info_f for proper usages.").queue();
        event.getHook().deleteOriginal().queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), "Displays information about a faction.");
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.STRING, "faction_name", "The name of the faction").setRequired(true));
        return commandData;
    }

    private String formatFactionData(Faction faction) {
        StringBuilder builder = new StringBuilder();
        builder.append("Info for ").append(faction.getName()).append(":\n");
        builder.append("Members: ").append(faction.getMembersUID().size()).append(":\n");
        builder.append("HomeBase: ");
        if(faction.getHomeSector() != null) builder.append("(").append(faction.getHomeSector().x).append(", ").append(faction.getHomeSector().y).append(", ").append(faction.getHomeSector().z).append(")");
        else builder.append("NONE");
        builder.append("\n");
        builder.append("Description: ").append(faction.getDescription());
        return builder.toString().trim();
    }
}
