package thederpgamer.starbridge.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.CommandData;
import net.dv8tion.jda.internal.interactions.CommandDataImpl;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;

import java.util.Objects;

/**
 * LinkCommand
 * <Description>
 *
 * @author Garret Reichenbach
 * @since 04/09/2021
 */
public class LinkCommand implements CommandInterface, DiscordCommand {

    @Override
    public String getCommand() {
        return "link";
    }

    @Override
    public String[] getAliases() {
        return new String[] {
                "link",
                "link%SEPARATOR%discord",
                "discord%SEPARATOR%link"
        };
    }

    @Override
    public String getDescription() {
        return "Gives you a special code that can be used to link your discord account to your StarMade account on SOE.\n" +
                "- /%COMMAND% : PMs you a link code. Use /link <code> in the #bot-commands discord channel to link your accounts.";
    }

    @Override
    public boolean isAdminOnly() {
        return false;
    }

    @Override
    public boolean onCommand(PlayerState playerState, String[] args) {
        StarBridge.getBot().addLinkRequest(playerState);
        return true;
    }

    @Override
    public void serverAction(@Nullable PlayerState playerState, String[] strings) {

    }

    @Override
    public StarMod getMod() {
        return StarBridge.getInstance();
    }

    @Override
    public void execute(SlashCommandInteractionEvent event) {
        try {
            int linkCode = Objects.requireNonNull(event.getOption("link_code")).getAsInt();
            PlayerData playerData = StarBridge.getBot().getLinkRequest(linkCode);
            if(playerData != null) {
                playerData.setDiscordId(event.getUser().getIdLong());
                ServerDatabase.updatePlayerData(playerData);
                PersistentObjectUtil.save(StarBridge.getInstance().getSkeleton());
                String logMessage = "Successfully linked user " + event.getUser().getName() + " to " + playerData.getPlayerName();
                StarBridge.getBot().removeLinkRequest(playerData);
                event.reply(logMessage).complete();
                StarBridge.getInstance().logInfo(logMessage);
                event.getHook().deleteOriginal().complete();
            }
        } catch(Exception exception) {
            StarBridge.getInstance().logException("Failed to link user " + event.getUser().getName() + " to an in-game account.", exception);
        }
    }

    @Override
    public CommandData getCommandData() {
        CommandDataImpl commandData = new CommandDataImpl(getCommand(), "Links your Discord and StarMade accounts using the provided link code");
        commandData.addOption(OptionType.INTEGER, "link_code", "The link code to use", true);
        return commandData;
    }
}
