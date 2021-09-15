package thederpgamer.starbridge.utils;

import api.common.GameClient;
import api.common.GameCommon;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.manager.LogManager;

/**
 * DataUtils
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/10/2021
 */
public class DataUtils {

    public static String getResourcesPath() {
        return StarBridge.getInstance().getSkeleton().getResourcesFolder().getPath().replace('\\', '/');
    }

    public static String getWorldDataPath() {
        String universeName = GameCommon.getUniqueContextId();
        if(!universeName.contains(":")) {
            return getResourcesPath() + "/data/" + universeName;
        } else {
            try {
                LogManager.logWarning("Client " + GameClient.getClientPlayerState().getName() + " attempted to illegally access server data.", null);
            } catch(Exception ignored) { }
            return null;
        }
    }
}
