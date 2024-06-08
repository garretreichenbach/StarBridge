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
	private final String[] watchList = {
		"error",
		"exception",
		"warning",
		"severe",
		"fatal",
		"critical",
		"failed",
		"invalid",
		"illegal",
		"throw",
		"fail",
		"runtime"
	};
	private final String[] filteredMessages = {
		"not an error",
		"server shutdown: false",
		"server shutdown: true",
		"server created: true",
		"sockettimeoutexception",
		"connection reset",
		"no content",
		"invalid blueprint",
		"trading.tag",
		"d_info",
		"fae",
		"npcspawnexception"
	};

	public LogWatcher(@NotNull OutputStream out) {
		super(out);
	}

	@Override
	public void print(String str) {
		super.print(str);
		for(String filteredMessage : filteredMessages) if(str.toLowerCase(Locale.ENGLISH).contains(filteredMessage)) return;
		for(String watch : watchList) {
			if(str.toLowerCase(Locale.ENGLISH).contains(watch.toLowerCase(Locale.ENGLISH))) {
				switch(watch) {
					case "error":
					case "exception":
					case "throw":
					case "fail":
						MessageType.LOG_EXCEPTION.sendMessage(str);
						return;
					case "fatal":
					case "critical":
					case "severe":
					case "runtime":
						MessageType.LOG_FATAL.sendMessage(str);
						return;
					case "warning":
						MessageType.LOG_WARNING.sendMessage(str);
						return;
				}
			}
		}
//		MessageType.LOG_INFO.sendMessage(str);
	}
}
