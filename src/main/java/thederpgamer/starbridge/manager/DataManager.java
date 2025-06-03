package thederpgamer.starbridge.manager;

import api.common.GameCommon;
import org.json.JSONObject;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.JsonSerializable;
import thederpgamer.starbridge.data.permissions.PermissionGroup;
import thederpgamer.starbridge.data.player.PlayerData;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * Manages serializable data related to players and groups in StarBridge.
 *
 * @author TheDerpGamer
 */
public class DataManager {

	public enum DataType {
		PLAYER(PlayerData.class),
		GROUP(PermissionGroup.class);

		private final Class<? extends JsonSerializable> dataClass;

		DataType(Class<? extends JsonSerializable> dataClass) {
			this.dataClass = dataClass;
		}

		public Class<? extends JsonSerializable> getDataClass() {
			return dataClass;
		}
	}

	public static String getResourcesPath() {
		return StarBridge.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
	}

	public static String getWorldDataPath() {
		String universeName = GameCommon.getUniqueContextId();
		if(!universeName.contains(":")) return getResourcesPath() + "/data/" + universeName;
		return null;
	}

	public static JsonSerializable getDataByUID(DataType dataType, String uid) {
		if(dataType == null || uid == null || uid.isEmpty()) return null;
		String dataPath = getWorldDataPath() + "/" + dataType.name().toLowerCase() + "/" + uid + ".json";
		File dataFile = new File(dataPath);
		if(!dataFile.exists()) return null;
		try {
			String jsonData = new String(Files.readAllBytes(dataFile.toPath()), StandardCharsets.UTF_8);
			JSONObject jsonObject = new JSONObject(jsonData);
			return dataType.getDataClass().getDeclaredConstructor(JSONObject.class).newInstance(jsonObject);
		} catch(InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException exception) {
			StarBridge.getInstance().logException("Failed to instantiate data class: " + dataType.getDataClass().getName(), exception);
			return null;
		} catch(Exception exception) {
			StarBridge.getInstance().logException("Failed to read data file: " + dataPath, exception);
			return null;
		}
	}

	public static Set<JsonSerializable> getAllData(DataType dataType) {
		if(dataType == null) return Collections.emptySet();
		String dataPath = getWorldDataPath() + "/" + dataType.name().toLowerCase();
		File dataDir = new File(dataPath);
		if(!dataDir.exists() || !dataDir.isDirectory()) return Collections.emptySet();
		Set<JsonSerializable> dataSet = new HashSet<>();
		for(File file : Objects.requireNonNull(dataDir.listFiles())) {
			if(file.isFile() && file.getName().endsWith(".json")) {
				try {
					String jsonData = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
					JSONObject jsonObject = new JSONObject(jsonData);
					JsonSerializable dataInstance = dataType.getDataClass().getDeclaredConstructor().newInstance();
					dataInstance.fromJson(jsonObject);
					dataSet.add(dataInstance);
				} catch(Exception exception) {
					StarBridge.getInstance().logException("Failed to read data file: " + file.getPath(), exception);
				}
			}
		}
		return dataSet;
	}

	public static void saveData(JsonSerializable data) {
		if(data == null) return;
		DataType dataType = DataType.valueOf(data.getClass().getSimpleName().toUpperCase());
		String dataPath = getWorldDataPath() + "/" + dataType.name().toLowerCase() + "/" + data.getUid() + ".json";
		File dataFile = new File(dataPath);
		try {
			if(!dataFile.getParentFile().exists()) dataFile.getParentFile().mkdirs();
			Files.write(dataFile.toPath(), data.toJson().toString(4).getBytes(StandardCharsets.UTF_8));
		} catch(Exception exception) {
			StarBridge.getInstance().logException("Failed to save data file: " + dataPath, exception);
		}
	}
}
