package thederpgamer.starbridge.bot;

import thederpgamer.starbridge.StarBridge;

import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Locale;

/**
 * Captures any Exceptions thrown by the game or mods and logs them to the staff log channel.
 *
 * @author Garret Reichenbach
 */
public class BotLogger {

	public BotLogger() {
		try {
			PrintStream outLog = new PrintStream(System.out, true, Charset.defaultCharset().name()) {
				@Override
				public void println(String x) {
					super.println(x);
					if(isException(x.toLowerCase(Locale.ENGLISH))) StarBot.getInstance().logException(x);
				}
			};
			System.setOut(outLog);
			PrintStream errorLog = new PrintStream(System.err, true, Charset.defaultCharset().name()) {
				@Override
				public void println(String x) {
					super.println(x);
					if(isException(x.toLowerCase(Locale.ENGLISH))) StarBot.getInstance().logException(x);
				}
			};
			System.setErr(errorLog);
		} catch(Exception exception) {
			StarBridge.getInstance().logException("An exception occurred when trying to set up error log", exception);
		}
	}

	private boolean isException(String message) {
		if(message.contains("exception")) return !message.contains("regular") && !message.contains("elementkeymap") && !message.contains("big update");
		else return false;
	}
}