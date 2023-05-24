package thederpgamer.starbridge.bot;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * [Description]
 *
 * @author TheDerpGamer (TheDerpGamer#0027)
 */
public class BotLogger {

	private static final String LOG_PATH = "logs/logstarmade.0.log";
	private String lastException = "";

	public BotLogger() {
		File logFile = new File(LOG_PATH);
		Scanner scanner = null;
		try {
			scanner = new Scanner(logFile);
		} catch(FileNotFoundException exception) {
			exception.printStackTrace();
			return;
		}
		Scanner finalScanner = scanner;
		(new Thread(() -> {
			while(true) {
				try {
					Thread.sleep(100);
				} catch(InterruptedException exception) {
					exception.printStackTrace();
					return;
				}
				if(finalScanner.hasNextLine()) {
					String out = finalScanner.nextLine();
					if(!out.equals(lastException) && out.contains("Exception") && !out.contains("IOException")) {
						StarBot.getInstance().logException(out);
						lastException = out;
					}
				}
			}
		})).start();
	}
}