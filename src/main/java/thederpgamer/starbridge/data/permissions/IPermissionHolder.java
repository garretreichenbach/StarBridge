package thederpgamer.starbridge.data.permissions;

import thederpgamer.starbridge.data.other.Pair;

import java.util.Set;

/**
 * Represents an entity that can hold permissions.
 * This can be a player, faction, an entity, etc.
 *
 * @author TheDerpGamer
 */
public interface IPermissionHolder {

	/**
	 * Gets the permission value for the specified permission.
	 */
	Object getPermission(String node);

	/**
	 * Sets a permission for this holder.
	 */
	void givePermission(String node, Object value);

	/**
	 * Revokes a permission from this holder.
	 */
	void revokePermission(String node);

	/**
	 * Gets all permissions held by this entity.
	 */
	Set<Pair<String, Object>> getPermissions();
}