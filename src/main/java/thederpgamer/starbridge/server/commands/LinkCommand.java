package thederpgamer.starbridge.server.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Command;
import net.dv8tion.jda.api.events.interaction.SlashCommandEvent;
import net.dv8tion.jda.api.requests.restaction.CommandUpdateAction;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;
import thederpgamer.starbridge.utils.LogUtils;
import thederpgamer.starbridge.utils.MessageType;

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
        StarBridge.instance.botThread.getBot().addLinkRequest(playerState);
        return true;
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
        try {
            int linkCode = Integer.parseInt(Objects.requireNonNull(event.getOption("link_code")).getAsString());
            PlayerData playerData = StarBridge.instance.botThread.getBot().getLinkRequest(linkCode);
            if(playerData != null) {
                playerData.setDiscordId(event.getUser().getIdLong());
                ServerDatabase.updatePlayerData(playerData);
                PersistentObjectUtil.save(StarBridge.instance.getSkeleton());
                String logMessage = "Successfully linked user " + event.getUser().getName() + " to " + playerData.getPlayerName();
                StarBridge.instance.botThread.getBot().removeLinkRequest(playerData);
                event.reply(logMessage).queue();
                LogUtils.logMessage(MessageType.INFO, logMessage);
                event.getHook().deleteOriginal().queue();
                return;
            }
        } catch(Exception exception) {
            LogUtils.logException("Failed to link user " + event.getUser().getName() + " to an in-game account.", exception);
        }
        event.reply("Sorry, but that link code is invalid").queue();
    }

    @Override
    public CommandUpdateAction.CommandData getCommandData() {
        CommandUpdateAction.CommandData commandData = new CommandUpdateAction.CommandData(getCommand(), "Links your Discord and StarMade accounts using the provided link code");
        commandData.addOption(new CommandUpdateAction.OptionData(Command.OptionType.INTEGER, "link_code", "The link code to use").setRequired(true));
        return commandData;
    }
}
