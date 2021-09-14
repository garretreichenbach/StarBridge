package thederpgamer.starbridge.utils;

import thederpgamer.starbridge.StarBridge;

import java.util.Calendar;
import java.util.Date;

/**
 * <Description>
 *
 * @version 1.0 - [08/20/2021]
 * @author TheDerpGamer
 */
public class ServerUtils {

    public static String getNextRestart() {
        Date current = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(current);
        Date lastRestart = new Date(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH));
        int restartsPerDay = (int) (86400000L / StarBridge.instance.autoRestartFrequency);
        int hoursTillNext = (int) (Math.ceil((float) current.getHours() / restartsPerDay)) * restartsPerDay;
        lastRestart.setHours(hoursTillNext); //Todo: No idea if this is actually even remotely correct
        Date nextRestart = new Date(lastRestart.getTime() + StarBridge.instance.autoRestartFrequency);
        return DateUtils.getHMDifferenceFormatted(nextRestart);
    }
}
