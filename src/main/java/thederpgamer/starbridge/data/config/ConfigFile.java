package thederpgamer.starbridge.data.config;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * ConfigFile
 * <Description>
 */
public class ConfigFile {

	private final File file;
	private final HashMap<String, String> configValues;

	public ConfigFile(String filePath) {
		file = new File(filePath);
		configValues = new HashMap<>();
		loadConfig();
	}

	public void loadConfig() {
		try {
			if(!file.exists()) file.createNewFile();
			Scanner scan = new Scanner(file);
			while(scan.hasNext()) {
				String line = scan.nextLine();
				if(line.contains("#")) line = line.substring(0, line.indexOf('#'));
				String field = line.substring(0, line.indexOf('=') - 1).trim();
				String value = line.substring(line.indexOf('=') + 1).trim();
				configValues.put(field, value);
			}
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	public void saveConfig() {
		try {
			if(!file.exists()) file.createNewFile();
			FileWriter writer = new FileWriter(file);
			for(Map.Entry<String, String> entry : configValues.entrySet()) {
				writer.write(entry.getKey() + " = " + entry.getValue() + "\n");
			}
			writer.flush();
			writer.close();
		} catch(IOException exception) {
			exception.printStackTrace();
		}
	}

	public String getConfigurableValue(String path, String defaultVal) {
		String string = getString(path);
		if(string == null) {
			set(path, defaultVal);
			return defaultVal;
		}
		return string;
	}

	public int getConfigurableInt(String path, int defaultVal) {
		String str = getConfigurableValue(path, String.valueOf(defaultVal));
		return Integer.parseInt(str.trim());
	}

	public long getConfigurableLong(String path, int defaultVal) {
		String str = getConfigurableValue(path, String.valueOf(defaultVal));
		return Long.parseLong(str.trim());
	}

	public float getConfigurableFloat(String path, float defaultVal) {
		String str = getConfigurableValue(path, String.valueOf(defaultVal));
		return Float.parseFloat(str.trim());
	}

	public boolean getConfigurableBoolean(String path, boolean defaultVal) {
		String str = getConfigurableValue(path, String.valueOf(defaultVal));
		return Boolean.parseBoolean(str.trim());
	}

	public boolean getBoolean(String path) {
		return Boolean.parseBoolean(configValues.get(path.trim()));
	}

	public int getInt(String path) {
		return Integer.parseInt(configValues.get(path.trim()));
	}

	public double getDouble(String path) {
		return Double.parseDouble(configValues.get(path.trim()));
	}

	public String getString(String path) {
		return configValues.get(path.trim());
	}

	public void set(String path, Object value) {
		if(value == null) configValues.remove(path);
		else configValues.put(path.trim(), value.toString().trim());
	}

	public void setList(String path, ArrayList<String> list) {
		StringBuilder sb = new StringBuilder();
		for(String s : list) sb.append(s.trim()).append(", ");
		sb.deleteCharAt(sb.length() - 1);
		set(path, sb);
	}

	public ArrayList<String> getList(String path) {
		ArrayList<String> r = new ArrayList<>();
		String string = getString(path);
		if(string == null) return r;
		r.addAll(Arrays.asList(string.split(", ")));
		return r;
	}
}