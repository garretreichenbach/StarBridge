package thederpgamer.starbridge.bot;

import api.utils.StarRunnable;
import thederpgamer.starbridge.StarBridge;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * [Description]
 *
 * @author TheDerpGamer (MrGoose#0027)
 */

public class BotLogger {

	private final ConcurrentHashMap<String, OutputStream> streams = new ConcurrentHashMap<>();
	private int fails = 0;
	private final ByteArrayOutputStream output;

	public BotLogger() {
		output = new ByteArrayOutputStream();
		startRunner();
	}

	private void startRunner() {
		try {
			new StarRunnable() {
				@Override
				public void run() {
					String out = output.toString();
					//Go through each line, looking for exceptions
					List<String> lines = Arrays.asList(out.split("\r\n|\r|\n"));
					for(String line : lines) {
						if(line.contains("Exception")) StarBridge.getBot().logException(new Exception(line));
					}
				}
			}.runTimer(StarBridge.getInstance(), 5000); //Run every 5 seconds
		} catch(Exception exception) {
			exception.printStackTrace();
			if(fails <= 5) {
				fails ++;
				startRunner();
			} else System.err.println("Failed to start LogWatcher after 5 previous crashes. Please report this to the developer.");
		}
	}

	public void startWatch() {
		startWatch("/logs/logstarmade.0.log");
	}

	public void startWatch(String path) {
		String logName = path.substring(path.lastIndexOf("/") + 1);
		if(!streams.containsKey(logName)) {
			OutputStream outputStream;
			if(logName.equals("logstarmade.0.log")) outputStream = new OutputStreamCombiner(Arrays.asList(System.out, System.err, output));
			else outputStream = new ByteArrayOutputStream();
			PrintStream printStream = new PrintStream(outputStream);
			streams.put(logName, outputStream);
			if(logName.equals("logstarmade.0.log")) System.setOut(printStream);
		}
	}

	private static class OutputStreamCombiner extends OutputStream {
		private final List<OutputStream> outputStreams;

		public OutputStreamCombiner(List<OutputStream> outputStreams) {
			this.outputStreams = outputStreams;
		}

		public void write(int b) throws IOException {
			for(OutputStream os : outputStreams) os.write(b);
		}

		public void flush() throws IOException {
			for(OutputStream os : outputStreams) os.flush();
		}

		public void close() throws IOException {
			for(OutputStream os : outputStreams) os.close();
		}
	}
}