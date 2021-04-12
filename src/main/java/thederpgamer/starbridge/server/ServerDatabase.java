package thederpgamer.starbridge.server;

import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.data.player.PlayerData;
import java.util.ArrayList;

/**
 * ServerDatabase.java
 * <Description>
 *
 * @since 03/10/2021
 * @author TheDerpGamer
 */
public class ServerDatabase {

    private static final ModSkeleton instance = StarBridge.instance.getSkeleton();

    public static PlayerData getPlayerData(String playerName) {
        ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        for(Object dataObject : dataObjectList) {
            PlayerData playerData = (PlayerData) dataObject;
            if(playerData.getPlayerName().equals(playerName)) return playerData;
        }
        return null;
    }

    public static ArrayList<PlayerData> getAllPlayerData() {
        ArrayList<PlayerData> playerDataList = new ArrayList<>();
        ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        for(Object dataObject : dataObjectList) playerDataList.add((PlayerData) dataObject);
        return playerDataList;
    }

    public static void addNewPlayerData(String playerName) {
        PersistentObjectUtil.addObject(instance, new PlayerData(playerName));
    }

    public static void updatePlayerData(PlayerData playerData) {
        ArrayList<PlayerData> toRemove = new ArrayList<>();
        ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        for(Object dataObject : dataObjectList) {
            PlayerData pData = (PlayerData) dataObject;
            if(pData.getPlayerName().equals(playerData.getPlayerName())) toRemove.add(pData);
        }
        for(PlayerData remove : toRemove) PersistentObjectUtil.removeObject(instance, remove);
        PersistentObjectUtil.addObject(instance, playerData);
    }
}
