package thederpgamer.starbridge.server.commands;

import api.mod.StarMod;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
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
        if(args.length >= 1) {
            StringBuilder builder = new StringBuilder();
            for(String s : args) builder.append(s).append(" ");
            String factionName = builder.toString().trim().replace("\"", "");
            Faction faction = ServerDatabase.getFaction(factionName);
            if(faction != null) PlayerUtils.sendMessage(sender, formatFactionData(faction));
            else PlayerUtils.sendMessage(sender, "Faction \"" + factionName + "\" doesn't exist!");
            return true;
        } else return false;
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
        String factionName = event.getOption("faction_name").getAsString();
        Faction faction = ServerDatabase.getFaction(factionName.replace("\"", ""));
        if(faction != null) event.reply(formatFactionData(faction)).queue();
        else event.reply("Faction " + factionName + "doesn't exist!").queue();
    }

    @Override
    public CommandData getCommandData() {
        CommandDataImpl commandData = new CommandDataImpl(getCommand(), "Displays information about a faction.");
        commandData.addOption(OptionType.STRING, "faction_name", "The name of the faction", true);
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
