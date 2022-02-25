package thederpgamer.starbridge.manager;

import api.DebugFile;
import org.schema.game.network.objects.ChatMessage;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.server.bot.DiscordBot;
import thederpgamer.starbridge.utils.DataUtils;
import thederpgamer.starbridge.utils.DateUtils;

import javax.annotation.Nullable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Manages mod data logging.
 *
 * @version 2.0 - [09/11/2021]
 * @author TheDerpGamer
 */
public class LogManager {

    private enum MessageType {
        DEBUG("[DEBUG]: "),
        INFO("[INFO]: "),
        WARNING("[WARNING]: "),
        ERROR("[ERROR]: "),
        CRITICAL("[CRITICAL]: ");

        public String prefix;

        MessageType(String prefix) {
            this.prefix = prefix;
        }
    }

    private static FileWriter logWriter;
    private static FileWriter chatWriter;

    private static final ArrayList<String> logQueue = new ArrayList<>();

    public static void initialize() {
        {
            String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
            File logsFolder = new File(logFolderPath);
            if(!logsFolder.exists()) logsFolder.mkdirs();
            else {
                if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
                    File[] logFiles = new File[logsFolder.listFiles().length];
                    int j = logFiles.length - 1;
                    for(int i = 0; i < logFiles.length && j >= 0; i++) {
                        logFiles[i] = logsFolder.listFiles()[j];
                        j--;
                    }

                    for(File logFile : logFiles) {
                        String fileName = logFile.getName().replace(".txt", "");
                        int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log_") + 4)) + 1;
                        String newName = logFolderPath + "/log_" + logNumber + ".txt";
                        if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) logFile.renameTo(new File(newName));
                        else logFile.delete();
                    }
                }
            }
            try {
                File newLogFile = new File(logFolderPath + "/log_0.txt");
                if(newLogFile.exists()) newLogFile.delete();
                newLogFile.createNewFile();
                logWriter = new FileWriter(newLogFile);
            } catch(IOException exception) {
                exception.printStackTrace();
            }
        }

        {
            String chatFolderPath = DataUtils.getWorldDataPath() + "/chat-logs";
            File chatFolder = new File(chatFolderPath);
            if(!chatFolder.exists()) chatFolder.mkdirs();
            else {
                if(chatFolder.listFiles() != null && chatFolder.listFiles().length > 0) {
                    File[] chatFiles = new File[chatFolder.listFiles().length];
                    int j = chatFiles.length - 1;
                    for(int i = 0; i < chatFiles.length && j >= 0; i ++) {
                        chatFiles[i] = chatFolder.listFiles()[j];
                        j --;
                    }

                    for(File chatFile : chatFiles) {
                        String fileName = chatFile.getName().replace(".txt", "");
                        int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("chat-log_") + 9)) + 1;
                        String newName = chatFolderPath + "/chat-log_" + logNumber + ".txt";
                        if(logNumber < ConfigManager.getMainConfig().getInt("max-world-logs") - 1) chatFile.renameTo(new File(newName));
                        else chatFile.delete();
                    }
                }
            }

            try {
                File newChatLogFile = new File(chatFolderPath + "/chat-log_0.txt");
                if(newChatLogFile.exists()) newChatLogFile.delete();
                newChatLogFile.createNewFile();
                chatWriter = new FileWriter(newChatLogFile);
            } catch(IOException exception) {
                exception.printStackTrace();
            }
        }
    }

    public static void logInfo(String message) {
        logMessage(MessageType.INFO, message);
    }

    public static void logDebug(String message) {
        if(ConfigManager.getMainConfig().getBoolean("debug-mode")) logMessage(MessageType.DEBUG, message);
    }

    public static void logWarning(String message, @Nullable
            Exception exception) {
        if(exception != null) logMessage(MessageType.WARNING, message + ":\n" + exception.getMessage());
        else logMessage(MessageType.WARNING, message);
    }

    public static void logException(String message, Exception exception) {
        exception.printStackTrace();
        logMessage(MessageType.ERROR, message + ":\n" + exception.getMessage());
    }

    public static void logCritical(String message, Exception exception) {
        exception.printStackTrace();
        logMessage(MessageType.CRITICAL, message + ":\n" + exception.getMessage());
    }

    private static void logMessage(MessageType messageType, String message) {
        message = DiscordBot.sanitizeMessage(message);
        if(!logQueue.contains(message) || messageType.equals(MessageType.CRITICAL)) {
            StringBuilder builder = new StringBuilder();
            String prefix = "[" + DateUtils.getTimeFormatted() + "] " + messageType.prefix;
            try {
                builder.append(prefix);
                String[] lines = message.split("\n");
                if(lines.length > 1) {
                    for(int i = 0; i < lines.length; i++) {
                        builder.append(lines[i]);
                        if(i < lines.length - 1) {
                            if(i > 1) for(int j = 0; j < prefix.length(); j++) builder.append(" ");
                        }
                    }
                } else builder.append(message);
                StarBridge.getInstance().getBot().sendLogMessage(builder.toString());
                System.out.println(builder.toString());
                logWriter.append(builder.toString()).append("\n");
                logWriter.flush();
                DebugFile.log(builder.toString(), StarBridge.getInstance());
            } catch(IOException exception) {
                exception.printStackTrace();
            }

            if(logQueue.size() >= 5) logQueue.remove(logQueue.size() - 1); //Prevent spam from repeated messages
            logQueue.add(builder.toString());

            if(messageType.equals(MessageType.CRITICAL)) System.exit(1);
        }
    }

    public static void logCommand(String sender, String command) {
        String logMessage = "[" + DateUtils.getTimeFormatted() + "] [COMMAND FROM " + sender + "]: " + command;
        try {
            StarBridge.getInstance().getBot().sendLogMessage(logMessage);
            chatWriter.append(logMessage).append("\n");
            chatWriter.flush();
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void logChat(ChatMessage chatMessage, String channel) {
        try {
            StringBuilder builder = new StringBuilder();
            String prefix = "[" + channel + "] [" + chatMessage.sender + "]: ";
            builder.append(prefix);
            String message = DiscordBot.sanitizeMessage(chatMessage.text);
            String[] lines = message.split("\n");
            if(lines.length > 1) {
                for(int i = 0; i < lines.length; i ++) {
                    builder.append(lines[i]);
                    if(i < lines.length - 1) {
                        if(i > 1) for(int j = 0; j < prefix.length(); j ++) builder.append(" ");
                    }
                }
            } else builder.append(message);
            StarBridge.getInstance().getBot().sendLogMessage("[" + DateUtils.getTimeFormatted() + "] [CHAT]: " + builder.toString());
            chatWriter.append(builder.toString()).append("\n");
            chatWriter.flush();
        } catch(IOException var3) {
            var3.printStackTrace();
        }
    }

    public static int clearLogs(int amount) {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        int amountCleared = 0;
        if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 1) {
            amount = Math.min(logsFolder.listFiles().length - 1, amount);
            for(File logFile : logsFolder.listFiles()) {
                if(!logFile.getName().contains("chat")) {
                    String logName = logFile.getName().replace(".txt", "");
                    int logNumber = Integer.parseInt(logName.substring(logName.indexOf("log_") + 4));
                    if(logNumber != 0 && amountCleared < amount) {
                        logFile.delete();
                        amountCleared ++;
                    }
                }
            }
        }
        logQueue.clear();
        return amountCleared;
    }

    public static int clearLogs() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 1) return clearLogs(logsFolder.listFiles().length - 1);
        return 0;
    }

    public static int clearChat(int amount) {
        String logFolderPath = DataUtils.getWorldDataPath() + "/chat-logs";
        File logsFolder = new File(logFolderPath);
        int amountCleared = 0;
        if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 1) {
            amount = Math.min(logsFolder.listFiles().length - 1, amount);
            for(File logFile : logsFolder.listFiles()) {
                String logName = logFile.getName().replace(".txt", "");
                int logNumber = Integer.parseInt(logName.substring(logName.indexOf("chat-log_") + 9));
                if(logNumber != 0 && amountCleared < amount) {
                    logFile.delete();
                    amountCleared ++;
                }
            }
        }
        return amountCleared;
    }

    public static int clearChat() {
        String chatFolderPath = DataUtils.getWorldDataPath() + "/chat-logs";
        File chatFolder = new File(chatFolderPath);
        if(chatFolder.listFiles() != null && chatFolder.listFiles().length > 1) return clearLogs(chatFolder.listFiles().length - 1);
        return 0;
    }
}
