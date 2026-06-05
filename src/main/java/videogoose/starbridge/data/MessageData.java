package videogoose.starbridge.data;

import java.time.Instant;

/**
 * Canonical message envelope for all bridge-relayed messages.
 *
 * <p>Every message that crosses the Discord/game boundary is represented as a
 * {@code MessageData} record.  The {@link Origin} field is the authoritative
 * "where did this come from?" signal used by the echo guard in
 * {@link videogoose.starbridge.bot.DiscordBot} to prevent feedback loops.
 *
 * <p>Rules:
 * <ul>
 *   <li>GAME  – originated from an in-game player chat event; may be relayed to Discord.</li>
 *   <li>DISCORD – originated from a Discord user message; may be relayed to the game.</li>
 *   <li>BRIDGE – synthesized / injected by StarBridge itself; must NEVER be relayed
 *       onward in either direction.</li>
 * </ul>
 *
 * @author VideoGoose
 */
public record MessageData(Origin origin, String author, String content, Instant timestamp) {

    // -------------------------------------------------------------------------
    // Origin enum
    // -------------------------------------------------------------------------

    /**
     * Indicates where a message originated so the bridge can decide whether
     * relaying it would create a feedback loop.
     */
    public enum Origin {
        /** Originated from an in-game player chat event. */
        GAME,
        /** Originated from a Discord message sent by a human user. */
        DISCORD,
        /**
         * Synthesized / injected by StarBridge itself (server announcements,
         * event notifications, etc.).  A BRIDGE message must NEVER be relayed
         * onward — doing so would create the duplicate-message feedback loop.
         */
        BRIDGE
    }

    // -------------------------------------------------------------------------
    // Factory helpers
    // -------------------------------------------------------------------------

    /** Creates a GAME-origin envelope for a message typed by an in-game player. */
    public static MessageData fromGame(String author, String content) {
        return new MessageData(Origin.GAME, author, content, Instant.now());
    }

    /** Creates a DISCORD-origin envelope for a message sent by a Discord user. */
    public static MessageData fromDiscord(String author, String content) {
        return new MessageData(Origin.DISCORD, author, content, Instant.now());
    }

    /**
     * Creates a BRIDGE-origin envelope for a server-synthesized message.
     * The author is always "Server" for these injections.
     */
    public static MessageData bridge(String content) {
        return new MessageData(Origin.BRIDGE, "Server", content, Instant.now());
    }

    // -------------------------------------------------------------------------
    // Legacy inner enum – preserved for backward compatibility
    // External callers that reference videogoose.starbridge.data.MessageData.MessageType
    // still compile without modification.
    // -------------------------------------------------------------------------

    /**
     * Original message-type routing flags.
     *
     * @deprecated This enum was part of the old MessageData class and is kept
     *             only for backward compatibility.  Use {@link Origin} for all
     *             bridge-routing decisions going forward.
     */
    @Deprecated
    public enum MessageType {
        EVENT(true, true, true),
        BROADCAST(true, true, true),
        PM(false, true, true),
        FACTION_CHANNEL(false, true, true),
        PRIVATE_CHANNEL(false, true, true),
        PUBLIC_CHANNEL(true, true, true);

        public final boolean showInDiscord;
        public final boolean showInGame;
        public final boolean showInLog;

        MessageType(boolean showInDiscord, boolean showInGame, boolean showInLog) {
            this.showInDiscord = showInDiscord;
            this.showInGame = showInGame;
            this.showInLog = showInLog;
        }
    }
}
