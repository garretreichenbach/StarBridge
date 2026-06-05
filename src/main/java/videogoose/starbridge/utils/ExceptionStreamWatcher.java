package videogoose.starbridge.utils;

import org.jetbrains.annotations.NotNull;
import videogoose.starbridge.error.ErrorManager;

import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Wraps {@code System.out}/{@code System.err} to capture exceptions that StarMade
 * prints to the console (it ships only {@code slf4j-api} with no binding, so its
 * real error output goes to stdout/stderr, not SLF4J).
 * <p>
 * Unlike the old {@code LogWatcher} — which started on any line containing the word
 * "exception" and stopped at the first tab-less line — this assembles a full stack
 * trace block: it begins on a strict exception/error header line, accumulates
 * {@code at …}/{@code Caused by:}/{@code …} continuation lines, and flushes the
 * complete block to {@link ErrorManager} when the trace ends. Everything still
 * passes through to the underlying stream unchanged.
 *
 * @author VideoGoose
 */
public class ExceptionStreamWatcher extends PrintStream {

	/** Guards against re-entrant capture if reporting itself writes to the stream. */
	private static final ThreadLocal<Boolean> REPORTING = ThreadLocal.withInitial(() -> false);

	private boolean inBlock;
	private String currentClass;
	private String currentMessage;
	private StringBuilder block;

	public ExceptionStreamWatcher(@NotNull OutputStream out) {
		super(out, true);
	}

	// NOTE: only the println(...) overloads capture. PrintStream.println delegates
	// internally to print(...) for subclasses, so overriding print(String) as well
	// would feed every line twice. Exceptions are emitted via println (printStackTrace
	// uses println per frame), so println coverage is sufficient.

	@Override
	public void println(String s) {
		super.println(s);
		if(s != null) feed(s);
	}

	@Override
	public void println(Object o) {
		super.println(o);
		if(o != null) feed(String.valueOf(o));
	}

	private synchronized void feed(String text) {
		if(REPORTING.get()) return;
		// A single call may carry several lines (e.g. a printed multi-line string).
		for(String line : text.split("\n", -1)) {
			if(line.isEmpty()) continue;
			consume(line);
		}
	}

	private void consume(String line) {
		if(inBlock) {
			if(ErrorManager.isFrameLine(line)) {
				block.append(line).append('\n');
				return;
			}
			flushBlock();
			// fall through: this line may itself begin a new block
		}
		String[] header = ErrorManager.parseHeader(line);
		if(header != null) {
			inBlock = true;
			currentClass = header[0];
			currentMessage = header[1];
			block = new StringBuilder(line).append('\n');
		}
	}

	private void flushBlock() {
		if(!inBlock) return;
		String cls = currentClass;
		String msg = currentMessage;
		String full = block.toString();
		inBlock = false;
		currentClass = null;
		currentMessage = null;
		block = null;
		REPORTING.set(true);
		try {
			ErrorManager.report(cls, msg, full);
		} finally {
			REPORTING.set(false);
		}
	}
}
