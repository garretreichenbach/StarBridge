package thederpgamer.starbridge.data.player;

import api.common.GameCommon;
import org.json.JSONArray;
import org.json.JSONObject;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.JsonSerializable;
import thederpgamer.starbridge.data.other.Pair;
import thederpgamer.starbridge.data.permissions.IGroupMember;
import thederpgamer.starbridge.data.permissions.PermissionGroup;
import thederpgamer.starbridge.manager.DataManager;
import thederpgamer.starbridge.utils.PlayerUtils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a player's data in StarBridge, including their name, playtime, permissions, and group membership.
 * Implements IGroupMember to manage group-related functionality.
 * This class is serializable to JSON for persistence.
 *
 * @author TheDerpGamer
 */
public class PlayerData extends JsonSerializable implements IGroupMember {

	private static final byte VERSION = 0;
	private final Set<Pair<String, Object>> permissions = new HashSet<>();
	private String uid;
	private String playerName;
	private long playTime;
	private long discordId;
	private String lastIp;
	private String starmadeName;
	private String groupUid;

	public PlayerData(String playerName) {
		this.playerName = playerName;
		groupUid = "NONE";
		uid = UUID.randomUUID().toString();
		playTime = 0;
		discordId = -1;
	}

	public PlayerData(JSONObject json) {
		fromJson(json);
	}

	public static PlayerData getFromName(String name) {
		if(name == null || name.isEmpty()) return null;
		Set<JsonSerializable> dataSet = DataManager.getAllData(DataManager.DataType.PLAYER);
		for(JsonSerializable data : dataSet) {
			if(data instanceof PlayerData && ((PlayerData) data).playerName.equalsIgnoreCase(name)) {
				return (PlayerData) data;
			}
		}
		return null;
	}

	public String getPlayerName() {
		return playerName;
	}

	public void setPlayerName(String playerName) {
		this.playerName = playerName;
	}

	public String getFactionName() {
		return (!inFaction()) ? "No Faction" : getFaction().getName();
	}

	public Faction getFaction() {
		PlayerState playerState = PlayerUtils.getPlayerState();
		if(playerState != null) {
			int factionId = playerState.getFactionId();
			if(factionId > 0) return GameCommon.getGameState().getFactionManager().getFaction(factionId);
		}
		return null;
	}

	public boolean inFaction() {
		return getFaction() != null;
	}

	public double getHoursPlayed() {
		return (double) playTime / (1000 * 60 * 60);
	}

	public void updatePlayTime(long timeSinceLastUpdate) {
		if(GameCommon.getPlayerFromName(playerName) != null) playTime += timeSinceLastUpdate;
	}

	public long getDiscordId() {
		return discordId;
	}

	public void setDiscordId(long discordId) {
		this.discordId = discordId;
	}

	public String getIP() {
		if(lastIp == null) return "Unknown";
		else return lastIp;
	}

	public void setIP(String ip) {
		lastIp = ip;
	}

	public String getStarmadeName() {
		return starmadeName;
	}

	public void setStarMadeName(String starmadeName) {
		this.starmadeName = starmadeName;
	}

	@Override
	public String getUid() {
		return uid;
	}

	@Override
	public JSONObject toJson() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("version", VERSION);
		jsonObject.put("uuid", uid);
		jsonObject.put("playerName", playerName);
		jsonObject.put("playTime", playTime);
		jsonObject.put("discordId", discordId);
		jsonObject.put("lastIp", lastIp);
		jsonObject.put("starmadeName", starmadeName);
		JSONArray permissionsArray = new JSONArray();
		for(Pair<String, Object> permission : permissions) {
			JSONObject permissionObject = new JSONObject();
			permissionObject.put("node", permission.left);
			permissionObject.put("value", permission.right);
			permissionsArray.put(permissionObject);
		}
		jsonObject.put("permissions", permissionsArray);
		jsonObject.put("groupUid", groupUid);
		return jsonObject;
	}

	@Override
	public void fromJson(JSONObject jsonObject) {
		if(jsonObject.has("version")) {
			byte version = (byte) jsonObject.getInt("version");
			if(version != VERSION) throw new IllegalArgumentException("Unsupported PlayerData version: " + version);
		}
		uid = jsonObject.getString("uuid");
		playerName = jsonObject.getString("playerName");
		playTime = jsonObject.getLong("playTime");
		discordId = jsonObject.getLong("discordId");
		lastIp = jsonObject.optString("lastIp", null);
		starmadeName = jsonObject.optString("starmadeName", null);
		JSONArray permissionsArray = jsonObject.getJSONArray("permissions");
		for(int i = 0; i < permissionsArray.length(); i++) {
			JSONObject permissionObject = permissionsArray.getJSONObject(i);
			String node = permissionObject.getString("node");
			Object value = permissionObject.get("value");
			permissions.add(new Pair<>(node, value));
		}
		groupUid = jsonObject.optString("groupUid", "NONE");
	}

	@Override
	public Object getPermission(String node) {
		for(Pair<String, Object> permission : permissions) {
			if(permission.left.equals(node)) {
				return permission.right;
			}
		}
		return null;
	}

	@Override
	public void givePermission(String node, Object value) {
		for(Pair<String, Object> permission : permissions) {
			if(permission.left.equals(node)) {
				permissions.remove(permission);
				break;
			}
		}
		permissions.add(new Pair<>(node, value));
	}

	@Override
	public void revokePermission(String node) {
		permissions.removeIf(permission -> permission.left.equals(node));
	}

	@Override
	public Set<Pair<String, Object>> getPermissions() {
		return Collections.unmodifiableSet(permissions);
	}

	@Override
	public boolean inAnyGroup() {
		return !"NONE".equals(groupUid);
	}

	@Override
	public boolean inGroup(String groupUid) {
		return this.groupUid.equals(groupUid);
	}

	@Override
	public String getGroup() {
		return groupUid;
	}

	@Override
	public void setGroup(String groupUid) {
		if(groupUid == null || groupUid.isEmpty()) this.groupUid = "NONE";
		else this.groupUid = groupUid;
	}

	@Override
	public Set<Pair<String, Object>> getPermissionsFromGroup() {
		Set<Pair<String, Object>> groupPermissions = new HashSet<>();
		if(inAnyGroup()) {
			PermissionGroup group = (PermissionGroup) DataManager.getDataByUID(DataManager.DataType.GROUP, groupUid);
			if(group != null) groupPermissions.addAll(group.getPermissions());
		}
		return Collections.unmodifiableSet(groupPermissions);
	}

	public String formatToNiceString() {
		StringBuilder builder = new StringBuilder();
		builder.append(playerName).append(":\n");
		builder.append("\tFaction: ").append(getFactionName()).append("\n");
		builder.append("\tTime played: ").append(String.format("%,.2f", (getHoursPlayed()))).append(" hours\n");
		if(discordId != -1) {
			try {
				String tag = StarBridge.getBot().getJDA().retrieveUserById(discordId).complete().getAsTag();
				builder.append("\tDiscord: ").append(tag).append("\n");
			} catch(Exception exception) {
				StarBridge.getInstance().logException("Error retrieving Discord tag for player: " + playerName, exception);
			}
		}
		builder.append("\tStatus: ");
		if(GameCommon.getPlayerFromName(playerName) != null) builder.append("ONLINE");
		else builder.append("OFFLINE");
		return builder.toString().trim();
	}
}
