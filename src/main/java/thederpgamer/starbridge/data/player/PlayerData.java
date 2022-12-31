package thederpgamer.starbridge.data.player;

import api.common.GameCommon;
import org.schema.game.common.data.player.PlayerState;
import org.schema.game.common.data.player.faction.Faction;
import thederpgamer.starbridge.utils.PlayerUtils;

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
    private boolean[] flags;
    private String lastIp;
    private String starmadeName;

    public PlayerData(String playerName) {
        this.playerName = playerName;
        this.playTime = 0;
        this.discordId = -1;
        this.flags = new boolean[] {true, false};
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setPlayerName(String playerName) {
        this.playerName = playerName;
    }

    public String getFactionName() {
        return (!inFaction()) ? "No Faction" : getFaction().getName();
    }

    public Faction getFaction() {
        PlayerState playerState = GameCommon.getPlayerFromName(playerName);
        if(playerState != null && playerState.getFactionId() > 0) return GameCommon.getGameState().getFactionManager().getFaction(playerState.getFactionId());
        else {
            try {
                return GameCommon.getGameState().getFactionManager().getFaction(PlayerUtils.getPlayerState(playerName).getFactionId());
            } catch(Exception exception) {
                exception.printStackTrace();
            }
        }
        return null;
    }

    public boolean inFaction() {
        return getFaction() != null;
    }

    public double getHoursPlayed() {
        return (double) playTime / (1000 * 60 * 60);
    }

    public void updatePlayTime(long timeSinceLastUpdate) {
        if(GameCommon.getPlayerFromName(playerName) != null) playTime += timeSinceLastUpdate;
    }

    public long getDiscordId() {
        return discordId;
    }

    public void setDiscordId(long discordId) {
        this.discordId = discordId;
    }

    public boolean isFirstDM() {
        return flags[0];
    }

    public void setFirstDM(boolean bool) {
        flags[0] = bool;
    }

    public boolean isExempt() {
        if(flags.length == 1) flags = new boolean[] {true, false};
        return flags[1];
    }

    public void setExempt(boolean bool) {
        if(flags.length == 1) flags = new boolean[] {true, false};
        flags[1] = bool;
    }

	public String getIP() {
        if(lastIp == null) return "Unknown";
        else return lastIp;
	}

    public void setIP(String ip) {
        lastIp = ip;
    }

    public String getStarmadeName() {
        return starmadeName;
    }

    public void setStarMadeName(String starmadeName) {
        this.starmadeName = starmadeName;
    }
}
