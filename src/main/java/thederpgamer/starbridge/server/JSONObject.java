package thederpgamer.starbridge.server;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import thederpgamer.starbridge.StarBridge;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * JSONObject
 */
public class JSONObject {

    private final HashMap<String, Object> map = new HashMap<String, Object>();

    public void put(String key, Object value) {
        if (value != null) map.put(key, value);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        Set<Map.Entry<String, Object>> entrySet = map.entrySet();
        builder.append("{");

        int i = 0;
        for (Map.Entry<String, Object> entry : entrySet) {
            Object val = entry.getValue();
            builder.append(quote(entry.getKey())).append(":");

            if (val instanceof String) {
                builder.append(quote(String.valueOf(val)));
            } else if (val instanceof Integer) {
                builder.append(Integer.valueOf(String.valueOf(val)));
            } else if (val instanceof Boolean) {
                builder.append(val);
            } else if (val instanceof JSONObject) {
                builder.append(val.toString());
            } else if (val.getClass().isArray()) {
                builder.append("[");
                int len = Array.getLength(val);
                for (int j = 0; j < len; j++) {
                    builder.append(Array.get(val, j).toString()).append(j != len - 1 ? "," : "");
                }
                builder.append("]");
            }

            builder.append(++i == entrySet.size() ? "}" : ",");
        }

        return builder.toString();
    }

    public void writeToURL(String url) {
        try {
            HttpClient httpClient = HttpClients.createDefault();
            HttpPost httpPost = new HttpPost(url);
            httpPost.addHeader("Content-Type", "application/json");
            httpPost.addHeader("User-Agent", "StarBridge (" + StarBridge.instance.botToken + ", " + StarBridge.instance.getSkeleton().getModVersion() + ")");
            httpPost.setEntity(new StringEntity(toString()));
            httpClient.execute(httpPost);
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    private String quote(String string) {
        return "\"" + string + "\"";
    }
}
