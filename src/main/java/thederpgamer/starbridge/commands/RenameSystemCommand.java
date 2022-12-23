package thederpgamer.starbridge.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.StellarSystem;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.bot.StarBot;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.utils.PlayerUtils;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class RenameSystemCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "rename_system";
	}

	@Override
	public String[] getAliases() {
		return new String[] {
				"rename_system"
		};
	}

	@Override
	public String getDescription() {
		return "Renames an owned system. Donators only!\n" +
				"- /%COMMAND% <new name>";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState playerState, String[] args) {
		if(args.length == 1) {
			PlayerData playerData = PlayerUtils.getPlayerData(playerState);
			if(playerData.getDiscordId() == 0) {
				api.utils.game.PlayerUtils.sendMessage(playerState, "You must be linked to Discord to use this command!");
				return true;
			} else {
				try {
					StellarSystem stellarSystem = GameServer.getServerState().getUniverse().getStellarSystemFromStellarPos(playerState.getCurrentSystem());
					User user = StarBot.getInstance().getBotThread().bot.getUserById(playerData.getDiscordId());
					Vector3i pos = stellarSystem.getPos();
					pos.add(-64,-64,-64);
					String centerOriginPos = pos.toString();
					assert user != null;
					if(stellarSystem.getOwnerFaction() == playerState.getFactionId()) {
						if(StarBot.getInstance().hasRole((Member) user, ConfigManager.getMainConfig().getLong("tier-1-role-id")) || StarBot.getInstance().hasRole((Member) user, ConfigManager.getMainConfig().getLong("tier-2-role-id"))) ConfigManager.getSystemNamesConfig().set(centerOriginPos, args[0]);
						else api.utils.game.PlayerUtils.sendMessage(playerState, "You must be a donator to use this command!");
					} else api.utils.game.PlayerUtils.sendMessage(playerState, "You must own the system to use this command!");
					return true;
				} catch(IOException exception) {
					exception.printStackTrace();
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
		return StarBridge.getInstance();
	}
}
