package thederpgamer.starbridge.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * DateUtils.java
 * Utility functions for date objects.
 *
 * @author TheDerpGamer
 * @since 03/20/2021
 */
public class DateUtils {

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
        difference -= (long) hours * 3600000L;

        int minutes = (int) (difference / 60000L);
        difference -= (long) minutes * 60000L;

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
}
