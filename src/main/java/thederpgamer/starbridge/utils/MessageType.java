package thederpgamer.starbridge.utils;

/**
 * MessageType
 * <Description>
 *
 * @author Garret Reichenbach
 * @since 04/10/2021
 */
public enum MessageType {
    INFO("[INFO]: "),
    WARNING("[WARNING]: "),
    ERROR("[ERROR]: "),
    CRITICAL("[CRITICAL]: ");

    public String prefix;

    MessageType(String prefix) {
        this.prefix = prefix;
    }
}