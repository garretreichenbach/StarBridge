package thederpgamer.starbridge.data.permissions;

import com.google.gson.JsonObject;
import javafx.util.Pair;
import org.json.JSONArray;
import org.json.JSONObject;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.JsonSerializable;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.DataManager;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Represents a group entity that can give permissions to its members.
 *
 * @author TheDerpGamer
 */
public class PermissionGroup extends JsonSerializable implements IPermissionHolder {

	private static final byte VERSION = 0;
	private String groupUid;
	private String groupName;
	private final Set<Pair<String, Object>> permissions = new HashSet<>();

	public PermissionGroup(String groupName) {
		this.groupName = groupName;
		groupUid = UUID.randomUUID().toString();
	}

	public PermissionGroup(JSONObject json) {
		fromJson(json);
	}

	public static PermissionGroup getFromName(String groupName) {
		if(groupName == null || groupName.isEmpty()) return null;
		Set<JsonSerializable> dataSet = DataManager.getAllData(DataManager.DataType.GROUP);
		for(JsonSerializable data : dataSet) {
			if(data instanceof PermissionGroup && ((PermissionGroup) data).getGroupName().equalsIgnoreCase(groupName)) {
				return (PermissionGroup) data;
			}
		}
		return null;
	}

	@Override
	public String getUid() {
		return groupUid;
	}

	@Override
	public JSONObject toJson() {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("version", VERSION);
		jsonObject.put("groupUid", groupUid);
		jsonObject.put("groupName", groupName);
		JSONArray permissionsArray = new JSONArray();
		for(Pair<String, Object> permission : permissions) {
			JSONObject permissionObject = new JSONObject();
			permissionObject.put("node", permission.getKey());
			permissionObject.put("value", permission.getValue());
			permissionsArray.put(permissionObject);
		}
		jsonObject.put("permissions", permissionsArray);
		JSONArray membersArray = new JSONArray();
		return jsonObject;
	}

	@Override
	public void fromJson(JSONObject jsonObject) {
		byte version = (byte) jsonObject.getInt("version");
		groupUid = jsonObject.getString("groupUid");
		groupName = jsonObject.getString("groupName");
		JSONArray permissionsArray = jsonObject.getJSONArray("permissions");
		for(int i = 0; i < permissionsArray.length(); i++) {
			JSONObject permissionObject = permissionsArray.getJSONObject(i);
			String node = permissionObject.getString("node");
			Object value = permissionObject.opt("value");
			permissions.add(new Pair<>(node, value));
		}
	}

	@Override
	public Object getPermission(String node) {
		for(Pair<String, Object> permission : permissions) {
			if(permission.getKey().equals(node)) return permission.getValue();
		}
		return null;
	}

	@Override
	public void givePermission(String node, Object value) {
		permissions.add(new Pair<>(node, value));
		DataManager.saveData(this);
	}

	@Override
	public void revokePermission(String node) {
		permissions.removeIf(permission -> permission.getKey().equals(node));
		DataManager.saveData(this);
	}

	@Override
	public Set<Pair<String, Object>> getPermissions() {
		return Collections.unmodifiableSet(permissions);
	}

	public String getGroupName() {
		return groupName;
	}

	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}

	public Set<String> getMemberUUIDs() {
		Set<JsonSerializable> allPlayers = DataManager.getAllData(DataManager.DataType.PLAYER);
		Set<String> memberUUIDs = new HashSet<>();
		for(JsonSerializable playerData : allPlayers) {
			if(playerData instanceof PlayerData) {
				PlayerData pd = (PlayerData) playerData;
				if(pd.getGroup().equals(groupUid)) memberUUIDs.add(pd.getUid());
			}
		}
		return Collections.unmodifiableSet(memberUUIDs);
	}

	public void addMember(String memberUUID) {
		PlayerData playerData = (PlayerData) DataManager.getDataByUID(DataManager.DataType.PLAYER, memberUUID); // Ensure the player data exists before adding
		if(playerData == null) {
			StarBridge.getInstance().logWarning("Attempted to add member with UUID " + memberUUID + " to group " + groupName + ", but player data does not exist.");
			return; // If player data does not exist, do not add to group
		}
		playerData.setGroup(groupUid);
		playerData.save();
	}

	public void addMember(PlayerData playerData) {
		if(playerData != null) addMember(playerData.getUid());
	}

	public void removeMember(String memberUUID) {
		PlayerData playerData = (PlayerData) DataManager.getDataByUID(DataManager.DataType.PLAYER, memberUUID);
		if(playerData == null) {
			StarBridge.getInstance().logWarning("Attempted to remove member with UUID " + memberUUID + " from group " + groupName + ", but player data does not exist.");
			return; // If player data does not exist, do not remove from group
		}
		playerData.setGroup("NONE"); // Reset group to NONE
	}

	public void removeMember(PlayerData playerData) {
		if(playerData != null) removeMember(playerData.getUid());
	}
}
