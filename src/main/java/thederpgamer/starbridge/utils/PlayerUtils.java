package thederpgamer.starbridge.utils;

import api.common.GameCommon;
import api.common.GameServer;
import org.apache.commons.lang3.math.NumberUtils;
import org.schema.game.common.controller.database.DatabaseIndex;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.server.data.GameServerState;
import org.schema.game.server.data.ServerConfig;
import org.schema.schine.resource.FileExt;
import org.schema.schine.resource.tag.Tag;
import thederpgamer.starbridge.data.player.PlayerData;
import thederpgamer.starbridge.manager.LogManager;
import thederpgamer.starbridge.server.ServerDatabase;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [01/30/2022]
 */
public class PlayerUtils {

    public enum EntryType {
        PLAYER_STATE("ENTITY_PLAYERSTATE");

        public final String databasePrefix;

        EntryType(String databasePrefix) {
            this.databasePrefix = databasePrefix;
        }
    }

    public static HashMap<String, Tag> getEntriesOfType(final EntryType entryType) {
        assert GameCommon.isOnSinglePlayer() || GameCommon.isDedicatedServer();
        File databaseFolder = new FileExt(GameServerState.ENTITY_DATABASE_PATH);
        if(databaseFolder.exists()) {
            try {
                File[] listFiles = databaseFolder.listFiles(new FilenameFilter() {
                    @Override
                    public boolean accept(File arg0, String name) {
                        return name.startsWith(entryType.databasePrefix);
                    }
                });

                if(listFiles != null) {
                    HashMap<String, Tag> entryMap = new HashMap<>();
                    for(File listFile : listFiles) {
                        String name = listFile.getName().substring(entryType.databasePrefix.length(), listFile.getName().lastIndexOf("."));
                        Tag tag = Tag.readFrom(new BufferedInputStream(new FileInputStream(listFile)), true, false);
                        entryMap.put(name, tag);
                    }
                    return entryMap;
                }
            } catch(Exception exception) {
                LogManager.logException("Encountered an exception while trying to fetch server database entries", exception);
            }
        }
        return new HashMap<>();
    }

    public static String[] getPlayerDatabaseFields(String playerName) {
        assert GameCommon.isOnSinglePlayer() || GameCommon.isDedicatedServer();
        String[] fields = new String[5];
        try {
            String maxNIOSize = ";hsqldb.nio_max_size=" + ServerConfig.SQL_NIO_FILE_SIZE.getCurrentState() + ";";
            Connection c = DriverManager.getConnection("jdbc:hsqldb:file:" + DatabaseIndex.getDbPath() + ";shutdown=true" + maxNIOSize, "SA", "");
            Statement s = c.createStatement();
            ResultSet q = s.executeQuery("SELECT * FROM PLAYERS;");
            while(q.next()) {
                String name = q.getString(1);
                if(name.equals(playerName)) {
                    fields = new String[] {
                            String.valueOf(q.getLong(0)),
                            name,
                            q.getString(2),
                            String.valueOf(q.getInt(3)),
                            String.valueOf(q.getLong(4))
                    };
                }
            }
        } catch(SQLException exception) {
            exception.printStackTrace();
        }
        return fields;
    }

    public static HashMap<PlayerState, PlayerData> getPlayerDataMap() {
        assert GameCommon.isOnSinglePlayer() || GameCommon.isDedicatedServer();
        HashMap<PlayerState, PlayerData> playerDataMap = new HashMap<>();
        try {
            HashMap<String, Tag> entries = getEntriesOfType(EntryType.PLAYER_STATE);
            for(Map.Entry<String, Tag> entry : entries.entrySet()) {
                try {
                    String[] fields = getPlayerDatabaseFields(entry.getKey());
                    PlayerState playerState = new PlayerState(GameServer.getServerState());
                    playerState.initialize();
                    playerState.fromTagStructure(entry.getValue());
                    playerState.setName(entry.getKey());
                    playerState.setStarmadeName(fields[2]);
                    try {
                        if(fields.length >= 4 && fields[3] != null && NumberUtils.isNumber(fields[3].trim())) {
                            int factionId = Integer.parseInt(fields[3].trim());
                            if(factionId > 0) playerState.getFactionController().setFactionId(factionId);
                            playerState.offlinePermssion[0] = factionId;
                            playerState.offlinePermssion[1] = Long.parseLong(fields[4].trim());
                        }
                    } catch(Exception exception) {
                        LogManager.logException("Encountered an exception while trying to fetch a player from database", exception);
                    }
                    playerDataMap.put(playerState, ServerDatabase.getPlayerData(playerState.getName()));
                } catch(Exception exception) {
                    LogManager.logException("Encountered an exception while trying to fetch a player from database", exception);
                }
            }
        } catch(Exception exception) {
            LogManager.logException("Encountered an exception while trying to fetch a player from database", exception);
        }
        return playerDataMap;
    }

    public static PlayerData getPlayerData(PlayerState playerState) {
        return getPlayerDataMap().get(playerState);
    }

    public static PlayerData getPlayerData(String playerName) {
        HashMap<PlayerState, PlayerData> playerDataMap = getPlayerDataMap();
        for(Map.Entry<PlayerState, PlayerData> entry : playerDataMap.entrySet()) {
            if(entry.getKey().getName().equals(playerName)) return entry.getValue();
        }
        return null;
    }

    public static PlayerState getPlayerState(String playerName) {
        HashMap<PlayerState, PlayerData> playerDataMap = getPlayerDataMap();
        for(Map.Entry<PlayerState, PlayerData> entry : playerDataMap.entrySet()) {
            if(entry.getKey().getName().equals(playerName)) return entry.getKey();
        }
        return null;
    }
}
