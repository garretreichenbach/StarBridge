package videogoose.starbridge.data;

import org.json.JSONObject;
import videogoose.starbridge.manager.DataManager;

/**
 * Interface defining the contract for objects that can be serialized to and deserialized from JSON.
 * All implementing classes must provide a unique UID, convert to JSONObject via toJson(), and reconstruct
 * from JSONObject via fromJson(). Objects also support toString() via JSON representation and can be
 * persisted via save().
 *
 * @author VideoGoose
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
