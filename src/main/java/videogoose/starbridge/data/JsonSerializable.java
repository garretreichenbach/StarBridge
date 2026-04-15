package videogoose.starbridge.data;

import org.json.JSONObject;
import videogoose.starbridge.manager.DataManager;

/**
 * [Description]
 *
 * @author TheDerpGamer
 */
public abstract class JsonSerializable {

	public abstract String getUid();

	public abstract JSONObject toJson();

	public abstract void fromJson(JSONObject jsonObject);

	public void save() {
		DataManager.saveData(this);
	}

	@Override
	public String toString() {
		return toJson().toString();
	}
}
