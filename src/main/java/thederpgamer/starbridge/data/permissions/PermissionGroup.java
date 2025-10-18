package thederpgamer.starbridge.data.permissions;

import org.json.JSONArray;
import org.json.JSONObject;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.JsonSerializable;
import thederpgamer.starbridge.data.other.Pair;
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
	private final Set<Pair<String, Object>> permissions = new HashSet<>();
	private final Set<String> inheritFrom = new HashSet<>();
	private String groupUid;
	private String groupName;

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
			if(data instanceof PermissionGroup && ((PermissionGroup) data).groupName.equalsIgnoreCase(groupName)) {
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
			permissionObject.put("node", permission.left);
			permissionObject.put("value", permission.right);
			permissionsArray.put(permissionObject);
		}
		jsonObject.put("permissions", permissionsArray);
		JSONArray inheritFromArray = new JSONArray();
		for(String inheritFrom : inheritFrom) {
			inheritFromArray.put(inheritFrom);
		}
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
		JSONArray inheritFromArray = jsonObject.getJSONArray("inheritFrom");
		for(int i = 0; i < inheritFromArray.length(); i++) {
			String inheritFrom = inheritFromArray.getString(i);
			addInheritFrom(inheritFrom);
		}
	}

	@Override
	public Object getPermission(String node) {
		for(Pair<String, Object> permission : permissions) {
			if(permission.left.equals(node)) return permission.right;
		}
		for(String inheritFrom : inheritFrom) {
			PermissionGroup group = (PermissionGroup) DataManager.getDataByUID(DataManager.DataType.GROUP, inheritFrom);
			if(group != null) {
				Object value = group.getPermission(node);
				if(value != null) return value;
			}
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
		permissions.removeIf(permission -> permission.left.equals(node));
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

	public void addInheritFrom(String inheritFrom) {
		this.inheritFrom.add(inheritFrom);
		DataManager.saveData(this);
	}

	public void removeInheritFrom(String inheritFrom) {
		this.inheritFrom.remove(inheritFrom);
	}

	public Set<String> getInheritFrom() {
		return Collections.unmodifiableSet(inheritFrom);
	}
}
