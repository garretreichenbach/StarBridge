package thederpgamer.starbridge.data;

import api.listener.events.player.PlayerChatEvent;
import org.schema.game.common.data.chat.ChannelRouter;
import org.schema.game.network.objects.ChatMessage;

/**
 * <Description>
 *
 * @author TheDerpGamer
 * @version 1.0 - [11/04/2021]
 */
public class MessageData {

	public String sender;
	public String text;
	public String channel;
	public MessageType messageType;

	public MessageData(PlayerChatEvent event) {
		ChatMessage message = event.getMessage();
		sender = message.sender;
		if(message.receiverType == ChatMessage.ChatMessageType.DIRECT && message.getChannel().getType() == ChannelRouter.ChannelType.DIRECT) {
			messageType = MessageType.PM;
		} else if(message.receiverType == ChatMessage.ChatMessageType.CHANNEL) {
			if(message.getChannel().getType() == ChannelRouter.ChannelType.PUBLIC) {
				messageType = MessageType.PUBLIC_CHANNEL;
				if(message.getChannel() != null && message.getChannel().getUniqueChannelName() != null) {
					channel = message.getChannel().getUniqueChannelName();
				} else {
					channel = "GENERAL"; //Todo: Figure out channel name of global chat
				}
			}
		}
	}

	public enum MessageType {
		EVENT(true, true, true),
		BROADCAST(true, true, true),
		PM(false, true, true),
		FACTION_CHANNEL(false, true, true),
		PRIVATE_CHANNEL(false, true, true),
		PUBLIC_CHANNEL(true, true, true);

		public boolean showInDiscord;
		public boolean showInGame;
		public boolean showInLog;

		MessageType(boolean showInDiscord, boolean showInGame, boolean showInLog) {
			this.showInDiscord = showInDiscord;
			this.showInGame = showInGame;
			this.showInLog = showInLog;
		}
	}
}
