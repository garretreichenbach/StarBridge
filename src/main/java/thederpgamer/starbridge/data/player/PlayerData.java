package thederpgamer.starbridge.data.player;

import api.common.GameCommon;
import org.schema.game.common.data.player.faction.Faction;
import java.io.Serializable;

/**
 * PlayerData.java
 * <Description>
 *
 * @since 03/10/2021
 * @author TheDerpGamer
 */
public class PlayerData implements Serializable {

    private String playerName;
    private long playTime;
    private long discordId;

    public PlayerData(String playerName) {
        this.playerName = playerName;
        this.playTime = 0;
        this.discordId = -1;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getFactionName() {
        return (getFaction() == null) ? "No Faction" : getFaction().getName();
    }

    public Faction getFaction() {
        if(inFaction()) {
            return GameCommon.getGameState().getFactionManager().getFaction(GameCommon.getPlayerFromName(playerName).getFactionId());
        } else {
            return null;
        }
    }

    public boolean inFaction() {
        return GameCommon.getPlayerFromName(playerName).getFactionId() != 0;
    }

    public double getHoursPlayed() {
        return (double) playTime / (1000 * 60 * 60);
    }

    public void updatePlayTime(long timeSinceLastUpdate) {
        playTime += timeSinceLastUpdate;
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }
}
