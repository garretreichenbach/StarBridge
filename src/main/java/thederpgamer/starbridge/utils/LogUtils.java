package thederpgamer.starbridge.utils;

import org.schema.game.network.objects.ChatMessage;
import thederpgamer.starbridge.StarBridge;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * LogUtils
 * <Description>
 *
 * @author Garret Reichenbach
 * @since 04/10/2021
 */
public class LogUtils {

    private static FileWriter logWriter;
    private static FileWriter chatWriter;

    public static void initialize() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(!logsFolder.exists()) logsFolder.mkdirs();
        else {
            if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
                File[] logFiles = new File[logsFolder.listFiles().length];
                int j = logFiles.length - 1;
                for(int i = 0; i < logFiles.length && j >= 0; i ++) {
                    logFiles[i] = logsFolder.listFiles()[j];
                    j --;
                }

                for(File logFile : logFiles) {
                    String fileName = logFile.getName().replace(".txt", "");
                    String newName;
                    int logNumber;
                    if(fileName.contains("chat-log")) {
                        logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("chat-log") + 8)) + 1;
                        newName = logFolderPath + "/chat-log" + logNumber + ".txt";
                    } else {
                        logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3)) + 1;
                        newName = logFolderPath + "/log" + logNumber + ".txt";
                    }
                    if(logNumber < StarBridge.instance.maxWorldLogs - 1) logFile.renameTo(new File(newName));
                    else logFile.delete();
                }
            }
        }
        try {
            File newLogFile = new File(logFolderPath + "/log0.txt");
            if(newLogFile.exists()) newLogFile.delete();
            newLogFile.createNewFile();
            logWriter = new FileWriter(newLogFile);

            File newChatLogFile = new File(logFolderPath + "/chat-log0.txt");
            if(newChatLogFile.exists()) newChatLogFile.delete();
            newChatLogFile.createNewFile();
            chatWriter = new FileWriter(newChatLogFile);
        } catch(IOException exception) {
            exception.printStackTrace();
        }
    }

    public static void logException(String message, Exception exception) {
        logMessage(MessageType.ERROR, message + ":\n" + exception.getMessage());
    }

    public static void logMessage(MessageType messageType, String message) {
        String prefix = "[" + DateUtils.getTimeFormatted() + "] [StarBridge] " + messageType.prefix;
        try {
            StringBuilder builder = new StringBuilder();
            builder.append(prefix);
            String[] lines = message.split("\n");
            if(lines.length > 1) {
                for(int i = 0; i < lines.length; i ++) {
                    builder.append(lines[i]);
                    if(i < lines.length - 1) {
                        if(i > 1) for(int j = 0; j < prefix.length(); j ++) builder.append(" ");
                    }
                }
            } else {
                builder.append(message);
            }
            System.out.println(builder.toString());
            logWriter.append(builder.toString()).append("\n");
            logWriter.flush();
        } catch(IOException var3) {
            var3.printStackTrace();
        }
        if(messageType.equals(MessageType.CRITICAL)) System.exit(1);
    }

    public static void logChat(ChatMessage chatMessage, String channel) {
        try {
            StringBuilder builder = new StringBuilder();
            String prefix = "[" + channel + "]" + "[" + chatMessage.sender + "]: ";
            builder.append(prefix);
            String message = chatMessage.text;
            String[] lines = message.split("\n");
            if(lines.length > 1) {
                for(int i = 0; i < lines.length; i ++) {
                    builder.append(lines[i]);
                    if(i < lines.length - 1) {
                        if(i > 1) for(int j = 0; j < prefix.length(); j ++) builder.append(" ");
                    }
                }
            } else builder.append(message);
            chatWriter.append(builder.toString()).append("\n");
            chatWriter.flush();
        } catch(IOException var3) {
            var3.printStackTrace();
        }
    }

    public static void clearLogs() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
            for(File logFile : logsFolder.listFiles()) {
                int logNumber;
                String logName = logFile.getName().replace(".txt", "");
                if(logFile.getName().contains("chat-log")) {
                    logNumber = Integer.parseInt(logFile.getName().substring(logName.indexOf("chat-log") + 8));
                } else {
                    logNumber = Integer.parseInt(logFile.getName().substring(logName.indexOf("log") + 3));
                }
                if(logNumber != 0 && logNumber - 1 >= StarBridge.instance.maxWorldLogs) logFile.delete();
            }
        }
    }
}
