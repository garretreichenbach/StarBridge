package thederpgamer.starbridge.server;

import api.common.GameCommon;
import api.common.GameServer;
import org.schema.game.common.data.chat.*;
import org.schema.game.common.data.player.PlayerState;

import java.util.ArrayList;

/**
 * ChatChannels
 * <Description>
 *
 * @author TheDerpGamer
 * @since 04/13/2021
 */
public enum ChatChannels {
    ALL,
    GENERAL,
    FACTION,
    PM,
    GROUP,
    STAFF;

    public ChatChannel toChatChannel() {
        return GameServer.getServerState().getChannelRouter().getChannel(toString().toUpperCase());
    }

    public static String fromChatChannel(ChatChannel chatChannel) {
        if(chatChannel instanceof AllChannel) return "[ALL]";
        else if(chatChannel instanceof PublicChannel) {
            if(chatChannel.getName() == null || (!chatChannel.getName().toLowerCase().contains("public") && !chatChannel.getName().toLowerCase().contains("general"))) {
                return "[GENERAL]";
            } else return "[" + chatChannel.getName() + "]";
        }
        else if(chatChannel instanceof FactionChannel) {
            String factionName = (GameCommon.getGameState().getFactionManager().getFaction(((FactionChannel) chatChannel).getFactionId())).getName();
            return "[FACTION: " + factionName + "]";
        } else if(chatChannel instanceof DirectChatChannel) {
            ArrayList<PlayerState> members = new ArrayList<>(chatChannel.getMemberPlayerStates());
            StringBuilder builder = new StringBuilder();
            builder.append("[PM: ");
            for(int i = 0; i < members.size(); i ++) {
                builder.append(members.get(i));
                if(i < members.size() - 1) builder.append(", ");
            }
            builder.append("]");
            return builder.toString();
        } else return "[GENERAL]";
    }
}
