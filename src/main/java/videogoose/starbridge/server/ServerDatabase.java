package videogoose.starbridge.server;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.faction.Faction;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.data.player.PlayerData;

import java.io.File;
import java.util.ArrayList;
import java.util.Objects;

public class ServerDatabase {

	private static final ModSkeleton instance = StarBridge.getInstance().getSkeleton();

	public static PlayerData getFromDiscordID(long discordID) {
		ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
		for(Object dataObject : dataObjectList) {
			PlayerData playerData = (PlayerData) dataObject;
			if(playerData.getDiscordId() == discordID) return playerData;
		}
		return null;
	}

	public static PlayerData getPlayerData(String playerName) {
		ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
		for(Object dataObject : dataObjectList) {
			PlayerData playerData = (PlayerData) dataObject;
			if(playerData.getPlayerName().equals(playerName)) return playerData;
		}
		return null;
	}

	public static PlayerData getPlayerDataOrCreateIfNull(String playerName) {
		ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
		for(Object dataObject : dataObjectList) {
			PlayerData playerData = (PlayerData) dataObject;
			if(playerData.getPlayerName().equals(playerName)) return playerData;
		}
		return addNewPlayerData(playerName);
	}

	public static ArrayList<PlayerData> getAllPlayerData() {
		ArrayList<PlayerData> playerDataList = new ArrayList<>();
		ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
		for(Object dataObject : dataObjectList) playerDataList.add((PlayerData) dataObject);
		return playerDataList;
	}

	/**
	 * Reports the state of the persistent player store at startup. The "new player" message
	 * wrongly firing for returning players after a restart means this store came back empty —
	 * logging the loaded count and the backing file (path/exists/size) pins down whether the
	 * data failed to load, was saved elsewhere, or never persisted. Loaded by StarLoader before
	 * {@code onEnable}, so the count here is authoritative.
	 */
	public static String persistenceDiagnostics() {
		int count = getAllPlayerData().size();
		String contextId = GameCommon.getUniqueContextId();
		File file = new File("moddata/" + instance.getName() + "/persistent/" + contextId + ".smdat");
		return "Player store: loaded " + count + " record(s); context='" + contextId + "'; file="
				+ file.getAbsolutePath() + " (exists=" + file.exists() + ", size=" + (file.exists() ? file.length() : 0) + " bytes)";
	}

	public static PlayerData addNewPlayerData(String playerName) {
		PlayerData playerData = new PlayerData(playerName);
		PersistentObjectUtil.addObject(instance, playerData);
		PersistentObjectUtil.save(instance);
		return playerData;
	}

	public static void updatePlayerData(PlayerData playerData) {
		ArrayList<PlayerData> toRemove = new ArrayList<>();
		ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
		for(Object dataObject : dataObjectList) {
			PlayerData pData = (PlayerData) dataObject;
			if(pData.getPlayerName().equals(playerData.getPlayerName()) || pData.getPlayerName().isEmpty()) toRemove.add(pData);
		}
		for(PlayerData remove : toRemove) PersistentObjectUtil.removeObject(instance, remove);
		PersistentObjectUtil.addObject(instance, playerData);
		PersistentObjectUtil.save(instance);
	}

	public static Faction getFaction(String factionName) {
		for(Faction faction : Objects.requireNonNull(GameCommon.getGameState()).getFactionManager().getFactionCollection()) {
			if(faction.getName().equals(factionName)) return faction;
		}
		return null;
	}
}
