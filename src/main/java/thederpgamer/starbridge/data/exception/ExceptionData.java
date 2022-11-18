package thederpgamer.starbridge.data.exception;

import api.mod.ModSkeleton;
import api.mod.StarLoader;
import api.mod.StarMod;
import api.mod.config.PersistentObjectUtil;
import thederpgamer.starbridge.StarBridge;
import thederpgamer.starbridge.bot.StarBot;

import java.util.ArrayList;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */
public class ExceptionData {

	public static ArrayList<ExceptionData> getExceptions() {
		ArrayList<ExceptionData> exceptionData = new ArrayList<>();
		for(Object object : PersistentObjectUtil.getObjects(StarBridge.getInstance().getSkeleton(), ExceptionData.class)) exceptionData.add((ExceptionData) object);
		return exceptionData;
	}

	private String name;
	private String description;
	private String modName;
	private String modVersion;
	private String exceptionClassName;
	private String[] stacktrace;
	private int severity;

	public ExceptionData(String exceptionClassName, String[] stacktrace) {
		//Search existing exception database for similar exceptions, if one is found use that data. If not, use the default data.
		ArrayList<ExceptionData> exceptions = getExceptions();
		for(ExceptionData exception : exceptions) {
			//Go through the static stacktrace and compare it to the current stacktrace, if they are more than 90% similar, use that exception data.
			int similarLines = 0;
			for(String line : exception.getStacktrace()) {
				for(String currentLine : stacktrace) {
					if(line.equals(currentLine)) similarLines ++;
				}
			}

			float similarity = (float) similarLines / (float) exception.getStacktrace().length;
			if(similarity >= 0.9f) { //This is a known exception
				this.name = exception.getName();
				this.description = exception.getDescription();
				this.modName = exception.getModName();
				this.modVersion = exception.getModVersion();
				this.exceptionClassName = exceptionClassName;
				this.stacktrace = stacktrace;
				//This exception has occurred before, increase the severity
				this.severity = exception.getSeverity() + 1;
				PersistentObjectUtil.removeObject(StarBridge.getInstance().getSkeleton(), exception);
				PersistentObjectUtil.addObject(StarBridge.getInstance().getSkeleton(), this);
			} else { //This is a new exception, save it
				//Figure out the mod name and version
				this.name = exception.getName();
				this.modName = "Unknown";
				this.modVersion = "Unknown";
				for(ModSkeleton mod : StarLoader.starMods) {
					//Get the package of the mod's main class
					String modPackage = mod.getMainClass().getClass().getPackage().getName();
					//Go through the stacktrace and see if any of the lines contain the mod's package
					for(String line : stacktrace) {
						if(line.contains(modPackage)) {
							this.modName = mod.getName();
							this.modVersion = mod.getModVersion();
							break;
						}
					}
				}
				this.name = "New Exception in " + this.modName + "v" + this.modVersion;
				this.description = "This is a new exception that has not been added to the exception database yet. Please report this exception to the mod author.";
				PersistentObjectUtil.addObject(StarBridge.getInstance().getSkeleton(), this);
			}
		}
		PersistentObjectUtil.save(StarBridge.getInstance().getSkeleton());
	}

	public ExceptionData(String name, String description, String modName, String modVersion, String exceptionClassName, String[] stacktrace) {
		this.name = name;
		this.description = description;
		this.modName = modName;
		this.modVersion = modVersion;
		this.exceptionClassName = exceptionClassName;
		this.stacktrace = stacktrace;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getModName() {
		return modName;
	}

	public void setModName(String modName) {
		this.modName = modName;
	}

	public String getModVersion() {
		return modVersion;
	}

	public void setModVersion(String modVersion) {
		this.modVersion = modVersion;
	}

	public String getExceptionClassName() {
		return exceptionClassName;
	}

	public void setExceptionClassName(String exceptionClassName) {
		this.exceptionClassName = exceptionClassName;
	}

	public String[] getStacktrace() {
		return stacktrace;
	}

	public void setStacktrace(String[] stacktrace) {
		this.stacktrace = stacktrace;
	}

	public int getSeverity() {
		return severity;
	}

	public void setSeverity(int severity) {
		this.severity = severity;
	}
}
