package thederpgamer.starbridge.utils;

import org.jetbrains.annotations.NotNull;
import thederpgamer.starbridge.bot.MessageType;

import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Locale;

/**
 * Log watcher that looks for errors or exceptions and sends them to the Discord bot.
 *
 * @author Garret Reichenbach
 */
public class LogWatcher extends PrintStream {

	private boolean printing;
	private StringBuilder builder;

	public LogWatcher(@NotNull OutputStream out) {
		super(out);
	}

	@Override
	public void print(String str) {
		super.print(str);
		if(printing) {
			builder.append(str);
			if(!str.contains("\t")) {
				MessageType.LOG_EXCEPTION.sendMessage(builder.toString());
				printing = false;
			}
		} else if(str.toLowerCase(Locale.ENGLISH).contains("exception")) {
			builder = new StringBuilder(str);
			printing = true;
		}
	}
}
