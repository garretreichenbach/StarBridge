package videogoose.starbridge.error;

/**
 * A single deduplicated error pattern tracked by {@link ErrorManager}.
 * <p>
 * Entries are runtime state (not persisted) keyed by a stable {@link #fingerprint}
 * derived from the exception class and its normalized top stack frames, so the
 * same recurring error always collapses onto one entry regardless of varying
 * messages (paths, ids, timestamps).
 *
 * @author Garret Reichenbach
 */
public class ErrorEntry {

	private final String fingerprint;
	private final String exceptionClass;
	private final String message;
	private final String fullText;
	private final long firstSeen;

	private long lastSeen;
	private int count;

	/** Sliding rate-limit window: start time and posts emitted within it. */
	private long postWindowStart;
	private int postWindowCount;
	private long lastPostedAt;

	public ErrorEntry(String fingerprint, String exceptionClass, String message, String fullText, long now) {
		this.fingerprint = fingerprint;
		this.exceptionClass = exceptionClass;
		this.message = message;
		this.fullText = fullText;
		this.firstSeen = now;
		this.lastSeen = now;
		this.count = 0;
	}

	public String getFingerprint() {
		return fingerprint;
	}

	public String getExceptionClass() {
		return exceptionClass;
	}

	public String getMessage() {
		return message;
	}

	public String getFullText() {
		return fullText;
	}

	public long getFirstSeen() {
		return firstSeen;
	}

	public long getLastSeen() {
		return lastSeen;
	}

	public int getCount() {
		return count;
	}

	/** Records another occurrence at {@code now}, returning the new count. */
	public int recordOccurrence(long now) {
		lastSeen = now;
		return ++count;
	}

	public long getLastPostedAt() {
		return lastPostedAt;
	}

	public void markPosted(long now) {
		lastPostedAt = now;
	}

	public long getPostWindowStart() {
		return postWindowStart;
	}

	public void setPostWindowStart(long postWindowStart) {
		this.postWindowStart = postWindowStart;
	}

	public int getPostWindowCount() {
		return postWindowCount;
	}

	public void setPostWindowCount(int postWindowCount) {
		this.postWindowCount = postWindowCount;
	}

	/** A short human-friendly label, e.g. {@code FileNotFoundException}. */
	public String simpleClassName() {
		var idx = exceptionClass.lastIndexOf('.');
		return idx >= 0 ? exceptionClass.substring(idx + 1) : exceptionClass;
	}
}
