package videogoose.starbridge.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DateUtils.java
 * Utility functions for date objects.
 *
 * @author VideoGoose
 */
public class DateUtils {

	public static long getDate(int month, int day, int year, int hours, int minutes) {
		return (new Date(year - 1900, month, day, hours, minutes, 0)).getTime();
	}

	public static int getAgeDays(Date date) {
		Date current = new Date(System.currentTimeMillis());
		long difference = Math.abs(current.getTime() - date.getTime());
		return (int) (difference / (1000 * 60 * 60 * 24));
	}

	public static int getAgeDays(long time) {
		return getAgeDays(new Date(time));
	}

	public static int getAgeHours(Date date) {
		Date current = new Date(System.currentTimeMillis());
		long difference = Math.abs(current.getTime() - date.getTime());
		return (int) (difference / (1000 * 60 * 60));
	}

	public static int getAgeHours(long time) {
		return getAgeHours(new Date(time));
	}

	public static String getHMDifferenceFormatted(Date date) {
		Date current = new Date(System.currentTimeMillis());
		long difference = Math.abs(date.getTime() - current.getTime());

		int hours = (int) (difference / 3600000L);
		difference -= hours * 3600000L;

		int minutes = (int) (difference / 60000L);
		difference -= minutes * 60000L;

		return hours + " hours " + minutes + " minutes";
	}

	public static String getHMDifferenceFormatted(long time) {
		return getHMDifferenceFormatted(new Date(time));
	}

	public static String getTimeFormatted(String format) {
		return (new SimpleDateFormat(format)).format(new Date());
	}

	public static String getTimeFormatted() {
		return getTimeFormatted("MM/dd/yyyy '-' hh:mm:ss z");
	}

	/**
	 * Format a duration in milliseconds into a compact human-readable string.
	 *
	 * <p>Breaks the duration into days, hours, minutes, and seconds, showing
	 * only the non-zero leading units. For example, {@code 3661000} becomes
	 * {@code "1h 1m 1s"}. Sub-second durations return {@code "0s"}.</p>
	 *
	 * @param millis the duration in milliseconds (treated as non-negative)
	 * @return a compact formatted string like {@code "2d 3h 4m 5s"}
	 */
	public static String formatDuration(long millis) {
		long remaining = Math.abs(millis);

		long days = remaining / 86400000L;
		remaining -= days * 86400000L;
		long hours = remaining / 3600000L;
		remaining -= hours * 3600000L;
		long minutes = remaining / 60000L;
		remaining -= minutes * 60000L;
		long seconds = remaining / 1000L;

		StringBuilder sb = new StringBuilder();
		if (days > 0) sb.append(days).append("d ");
		if (days > 0 || hours > 0) sb.append(hours).append("h ");
		if (days > 0 || hours > 0 || minutes > 0) sb.append(minutes).append("m ");
		sb.append(seconds).append("s");
		return sb.toString();
	}
}
