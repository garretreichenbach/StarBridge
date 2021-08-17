package thederpgamer.starbridge.server;

import api.common.GameCommon;
import api.mod.ModSkeleton;
import api.mod.config.PersistentObjectUtil;
import org.schema.game.common.data.player.faction.Faction;
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
        return addNewPlayerData(playerName);
    }

    public static ArrayList<PlayerData> getAllPlayerData() {
        ArrayList<PlayerData> playerDataList = new ArrayList<>();
        ArrayList<Object> dataObjectList = PersistentObjectUtil.getObjects(instance, PlayerData.class);
        for(Object dataObject : dataObjectList) playerDataList.add((PlayerData) dataObject);
        return playerDataList;
    }

    public static PlayerData addNewPlayerData(String playerName) {
        PlayerData playerData = new PlayerData(playerName);
        PersistentObjectUtil.addObject(instance, playerData);
        return playerData;
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

    public static Faction getFaction(String factionName) {
        for(Faction faction : GameCommon.getGameState().getFactionManager().getFactionCollection()) {
            if(faction.getName().equals(factionName)) return faction;
        }
        return null;
    }
}
