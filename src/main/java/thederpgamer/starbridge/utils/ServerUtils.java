package thederpgamer.starbridge.utils;

import api.mod.ModSkeleton;
import api.mod.StarLoader;
import api.utils.GameRestartHelper;
import api.utils.StarRunnable;
import org.schema.schine.network.server.ServerController;
import thederpgamer.starbridge.StarBridge;

import java.util.ArrayList;

/**
 * ServerUtils
 * <Description>
 *
 * @author TheDerpGamer
 * @since 08/20/2021
 */
public class ServerUtils {

    public static void triggerRestart() {
        new StarRunnable() {
            @Override
            public void run() {
                try {
                    StringBuilder sb = new StringBuilder();
                    for(Integer modId : getEnabledModIds()) sb.append(modId).append(",");
                    sb.deleteCharAt(sb.length() - 1);
                    GameRestartHelper.runWithArguments(new String[] {"-server", "-port:" + ServerController.port, "-modded", "-forceupdate", "-autoupdatemods"}, sb.toString());
                } catch(Exception exception) {
                    LogUtils.logException("Encountered a critical error while trying to restart the server", exception);
                }
            }
        }.runLater(StarBridge.instance, 100L);
    }

    private static ArrayList<Integer> getEnabledModIds() {
        ArrayList<Integer> enabledMods = new ArrayList<>();
        for(ModSkeleton skeleton : StarLoader.starMods) {
            if(skeleton.isEnabled()) enabledMods.add(skeleton.getSmdResourceId());
        }
        return enabledMods;
    }
}
