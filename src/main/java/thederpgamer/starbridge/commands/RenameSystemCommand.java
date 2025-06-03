package thederpgamer.starbridge.commands;

import api.common.GameServer;
import api.mod.StarMod;
import api.utils.game.chat.CommandInterface;
import org.schema.common.util.linAlg.Vector3i;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.world.StellarSystem;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.permissions.IPermissibleAction;
import thederpgamer.starbridge.data.permissions.PermissionGroup;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.ConfigManager;
import thederpgamer.starbridge.manager.DataManager;
import thederpgamer.starbridge.utils.PlayerUtils;

import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Renames an owned system.
 */
public class RenameSystemCommand implements CommandInterface, IPermissibleAction {
	@Override
	public String getCommand() {
		return "rename_system";
	}

	@Override
	public String[] getAliases() {
		return new String[] {"rename_system"};
	}

	@Override
	public String getDescription() {
		return "Renames an owned system." + "- /%COMMAND% <new name>";
	}

	@Override
	public boolean isAdminOnly() {
		return false;
	}

	@Override
	public boolean onCommand(PlayerState playerState, String[] args) {
		if(args.length == 1) {
			try {
				StellarSystem stellarSystem = GameServer.getServerState().getUniverse().getStellarSystemFromStellarPos(playerState.getCurrentSystem());
				Vector3i pos = stellarSystem.getPos();
				pos.add(-64, -64, -64);
				String centerOriginPos = pos.toString();
				PlayerData playerData = PlayerData.getFromName(playerState.getName());
				if(playerData != null) {
					boolean hasPermission = (boolean) playerData.getPermission("rename_system");
					if(stellarSystem.getOwnerFaction() != playerState.getFactionId() && !playerState.isAdmin()) {
						api.utils.game.PlayerUtils.sendMessage(playerState, "You do not have permission to rename this system!");
						return false;
					}
					if(!hasPermission && !ConfigManager.getMainConfig().getBoolean("debug-mode")) {
						api.utils.game.PlayerUtils.sendMessage(playerState, "You do not have permission to rename systems!");
						return false;
					}
					if(args[0].length() > 32) {
						api.utils.game.PlayerUtils.sendMessage(playerState, "System name cannot be longer than 32 characters!");
						return false;
					}

					ConfigManager.getSystemNamesConfig().set(centerOriginPos, args[0]);
					api.utils.game.PlayerUtils.sendMessage(playerState, "System name has been changed to: " + args[0] + ", will take effect on next server restart.");
				}
				return true;
			} catch(IOException exception) {
				StarBridge.getInstance().logException("Failed to rename system", exception);
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

	@Override
	public PermissionGroup getRequiredGroup() {
		return PermissionGroup.getFromName();
	}
}
