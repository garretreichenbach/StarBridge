package thederpgamer.starbridge.data.player;

import api.common.GameCommon;
import org.schema.game.common.data.physics.GamePhysicsObject;
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

    public PlayerData(String playerName) {
        this.playerName = playerName;
        this.playTime = 0;
        this.discordId = -1;
        this.flags = new boolean[] {true};
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
        try {
            return GameCommon.getGameState().getFactionManager().getFaction(PlayerUtils.getPlayerState(playerName).getFactionId());
        } catch(Exception ignored) { }
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
}
