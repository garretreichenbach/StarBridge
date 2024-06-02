package thederpgamer.starbridge.utils;

/**
 * Exception that is used for debug logging and doesn't actually mean an error occurred.
 *
 * @author Garret Reichenbach
 */
public class DebugException extends RuntimeException {

	private final Thread thread;

	public DebugException(String message, Thread thread) {
		super(message);
		this.thread = thread;
	}

	@Override
	public String getMessage() {
		return super.getMessage() + " (Thread: " + thread.getName() + ")";
	}
}
