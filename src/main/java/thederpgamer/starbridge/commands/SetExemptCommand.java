package thederpgamer.starbridge.commands;

import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import api.utils.game.PlayerUtils;
import api.utils.game.chat.CommandInterface;
import org.jetbrains.annotations.Nullable;
import org.schema.game.common.data.player.PlayerState;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.server.ServerDatabase;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class SetExemptCommand implements CommandInterface {
	@Override
	public String getCommand() {
		return "set_exempt";
	}

	@Override
	public String[] getAliases() {
		return new String[] {
				"set_exempt"
		};
	}

	@Override
	public String getDescription() {
		return "Sets a player to be exempt from the IP checker.\n" +
				" - /%COMMAND% <player> <true|false> : Sets a player to be exempt from the IP checker.";
	}

	@Override
	public boolean isAdminOnly() {
		return true;
	}

	@Override
	public boolean onCommand(PlayerState playerState, String[] strings) {
		if(strings.length == 2) {
			String playerName = strings[0];
			boolean exempt = Boolean.parseBoolean(strings[1]);
			PlayerData playerData = ServerDatabase.getPlayerDataWithoutNull(playerName);
			playerData.setExempt(exempt);
			PersistentObjectUtil.removeObject(StarBridge.getInstance().getSkeleton(), playerData);
			PersistentObjectUtil.addObject(StarBridge.getInstance().getSkeleton(), playerData);
			PersistentObjectUtil.save(StarBridge.getInstance().getSkeleton());
			PlayerUtils.sendMessage(playerState, "Set " + playerName + " to be exempt: " + exempt);
			return true;
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
