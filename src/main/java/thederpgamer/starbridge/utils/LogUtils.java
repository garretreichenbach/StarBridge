package thederpgamer.starbridge.utils;

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

    public static void initialize() {
        String logFolderPath = DataUtils.getWorldDataPath() + "/logs";
        File logsFolder = new File(logFolderPath);
        if(!logsFolder.exists()) logsFolder.mkdirs();
        else {
            if(logsFolder.listFiles() != null && logsFolder.listFiles().length > 0) {
                for(File logFile : logsFolder.listFiles()) {
                    String fileName = logFile.getName().replace(".txt", "");
                    int logNumber = Integer.parseInt(fileName.substring(fileName.indexOf("log") + 3));
                    if(logNumber < StarBridge.instance.maxWorldLogs - 1) logFile.renameTo(new File("log" + (logNumber + 1)));
                    else logFile.delete();
                }
            }
        }
        try {
            File newLogFile = new File(logFolderPath + "/log0.txt");
            if(newLogFile.exists()) newLogFile.delete();
            newLogFile.createNewFile();
            logWriter = new FileWriter(newLogFile);
        } catch(IOException exception) {
            exception.printStackTrace();
        }
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
}
