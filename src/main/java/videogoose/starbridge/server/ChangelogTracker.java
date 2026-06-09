package videogoose.starbridge.server;

import api.mod.ModSkeleton;
import api.mod.StarLoader;
import org.json.JSONObject;
import org.schema.game.common.version.Version;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.bot.MessageType;
import videogoose.starbridge.manager.DataManager;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Persists a snapshot of the server environment — the StarMade version/build and
 * the set of enabled mods with their versions — and, on each startup, diffs the
 * previous snapshot against the current one. Any differences (server update, or a
 * mod added, removed, or version-changed) are posted to the configured
 * {@code changelog-channel-id} Discord channel.
 *
 * <p>The snapshot lives at {@code moddata/StarBridge/server-snapshot.json} and is
 * server-global (not universe-scoped), since version and mod state are properties
 * of the server install rather than any individual universe.
 *
 * <p>The very first run (no prior snapshot) writes a baseline silently — there is
 * nothing to compare against, so nothing is posted.
 *
 * @author VideoGoose
 */
public class ChangelogTracker {

	private static final String SNAPSHOT_FILE = "server-snapshot.json";
	private static final String KEY_SERVER_VERSION = "serverVersion";
	private static final String KEY_SERVER_BUILD = "serverBuild";
	private static final String KEY_MODS = "mods";

	private ChangelogTracker() {
	}

	/**
	 * Compares the persisted snapshot to the current server state, posts a
	 * changelog if anything changed, then persists the current state as the new
	 * baseline. Failures are logged and swallowed so a tracking problem never
	 * blocks startup.
	 */
	public static void checkAndPost() {
		try {
			JSONObject current = captureCurrent();
			JSONObject previous = loadPrevious();

			if (previous != null) {
				List<String> changes = diff(previous, current);
				if (!changes.isEmpty()) {
					String body = "**Server changelog**\n" + String.join("\n", changes);
					MessageType.CHANGELOG.sendMessage(body);
				}
			}

			save(current);
		} catch (Exception exception) {
			StarBridge.getInstance().logException("Failed to update server changelog tracker", exception);
		}
	}

	// -------------------------------------------------------------------------
	// Snapshot capture / persistence
	// -------------------------------------------------------------------------

	private static JSONObject captureCurrent() {
		JSONObject snapshot = new JSONObject();
		snapshot.put(KEY_SERVER_VERSION, Version.VERSION);
		snapshot.put(KEY_SERVER_BUILD, Version.BUILD);

		// TreeMap keeps the JSON deterministically ordered by mod name.
		Map<String, String> mods = new TreeMap<>();
		for (ModSkeleton mod : StarLoader.starMods) {
			if (mod != null && mod.isEnabled()) {
				mods.put(mod.getName(), mod.getModVersion());
			}
		}
		snapshot.put(KEY_MODS, new JSONObject(mods));
		return snapshot;
	}

	private static JSONObject loadPrevious() {
		File file = snapshotFile();
		if (!file.exists()) return null;
		try {
			String json = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
			return new JSONObject(json);
		} catch (Exception exception) {
			StarBridge.getInstance().logException("Failed to read server snapshot: " + file.getPath(), exception);
			return null;
		}
	}

	private static void save(JSONObject snapshot) {
		File file = snapshotFile();
		try {
			if (file.getParentFile() != null && !file.getParentFile().exists()) {
				file.getParentFile().mkdirs();
			}
			Files.write(file.toPath(), snapshot.toString(4).getBytes(StandardCharsets.UTF_8));
		} catch (Exception exception) {
			StarBridge.getInstance().logException("Failed to write server snapshot: " + file.getPath(), exception);
		}
	}

	private static File snapshotFile() {
		return new File(DataManager.getResourcesPath() + "/" + SNAPSHOT_FILE);
	}

	// -------------------------------------------------------------------------
	// Diffing
	// -------------------------------------------------------------------------

	/**
	 * Builds a list of human-readable changelog lines describing how {@code current}
	 * differs from {@code previous}. Returns an empty list when nothing changed.
	 */
	private static List<String> diff(JSONObject previous, JSONObject current) {
		List<String> changes = new ArrayList<>();

		// Server version / build.
		String oldVersion = previous.optString(KEY_SERVER_VERSION, "");
		String newVersion = current.optString(KEY_SERVER_VERSION, "");
		String oldBuild = previous.optString(KEY_SERVER_BUILD, "");
		String newBuild = current.optString(KEY_SERVER_BUILD, "");
		if (!oldVersion.equals(newVersion) || !oldBuild.equals(newBuild)) {
			changes.add(":arrow_up: **Server** updated: `" + versionLabel(oldVersion, oldBuild)
					+ "` → `" + versionLabel(newVersion, newBuild) + "`");
		}

		// Mods. TreeMap so added/removed/updated lines come out name-sorted.
		Map<String, String> oldMods = toModMap(previous.optJSONObject(KEY_MODS));
		Map<String, String> newMods = toModMap(current.optJSONObject(KEY_MODS));

		Map<String, String> added = new TreeMap<>();
		Map<String, String[]> updated = new TreeMap<>();
		for (Map.Entry<String, String> entry : newMods.entrySet()) {
			if (!oldMods.containsKey(entry.getKey())) {
				added.put(entry.getKey(), entry.getValue());
			} else if (!oldMods.get(entry.getKey()).equals(entry.getValue())) {
				updated.put(entry.getKey(), new String[]{oldMods.get(entry.getKey()), entry.getValue()});
			}
		}
		Map<String, String> removed = new TreeMap<>();
		for (Map.Entry<String, String> entry : oldMods.entrySet()) {
			if (!newMods.containsKey(entry.getKey())) {
				removed.put(entry.getKey(), entry.getValue());
			}
		}

		for (Map.Entry<String, String> entry : added.entrySet()) {
			changes.add(":heavy_plus_sign: **Mod added:** `" + entry.getKey() + "` v" + entry.getValue());
		}
		for (Map.Entry<String, String> entry : removed.entrySet()) {
			changes.add(":heavy_minus_sign: **Mod removed:** `" + entry.getKey() + "` (was v" + entry.getValue() + ")");
		}
		for (Map.Entry<String, String[]> entry : updated.entrySet()) {
			changes.add(":arrows_counterclockwise: **Mod updated:** `" + entry.getKey()
					+ "` `" + entry.getValue()[0] + "` → `" + entry.getValue()[1] + "`");
		}

		return changes;
	}

	private static String versionLabel(String version, String build) {
		if (version.isEmpty()) version = "unknown";
		if (build.isEmpty()) return version;
		return version + " (build " + build + ")";
	}

	private static Map<String, String> toModMap(JSONObject mods) {
		Map<String, String> map = new TreeMap<>();
		if (mods != null) {
			for (java.util.Iterator<String> keys = mods.keys(); keys.hasNext(); ) {
				String key = keys.next();
				map.put(key, mods.optString(key, ""));
			}
		}
		return map;
	}
}
