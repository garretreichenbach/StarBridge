package videogoose.starbridge.error;

import api.mod.config.FileConfiguration;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.components.actionrow.ActionRow;
import net.dv8tion.jda.api.components.buttons.Button;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import videogoose.starbridge.StarBridge;
import videogoose.starbridge.manager.ConfigManager;
import videogoose.starbridge.utils.DiscordUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Deduplicates, rate-limits and reports server errors to Discord, and lets admins
 * silence noisy patterns. Replaces the old stdout-scraping {@code LogWatcher} +
 * buggy {@code ExceptionData}.
 * <p>
 * Captured errors (from the {@code ExceptionStreamWatcher} stdout/stderr tap, the
 * logback appender, and {@code StarBridge.logException}) are fingerprinted by
 * exception class + normalized top stack frames so recurring errors collapse onto
 * one {@link ErrorEntry}. A given fingerprint posts once, then at most once per
 * {@code min-post-interval-ms} with an updated occurrence count — so a fast-looping
 * error can never flood the log channel.
 * <p>
 * Mute rules (exact fingerprints, regex patterns, and per-pattern alert thresholds)
 * are persisted in the {@code errors} config so they survive restarts. Muting only
 * suppresses Discord reporting; it never changes game behavior, and fatal/crash
 * reports ({@link #reportFatal}) are never muted.
 *
 * @author VideoGoose
 */
public final class ErrorManager {

	public static final String BUTTON_PREFIX = "sb-err";

	private static final Pattern HEADER = Pattern.compile(
			"^(?:\\[[^\\]]*]\\s*)?(?:Exception in thread \"[^\"]*\"\\s+)?((?:[\\w$]+\\.)*[\\w$]+(?:Exception|Error|Throwable))(?::\\s*(.*))?$");
	private static final Pattern FRAME = Pattern.compile("^(?:\\[[^\\]]*]\\s*)?\\s*at\\s+(.+)$");
	private static final Pattern CAUSED_BY = Pattern.compile("^(?:\\[[^\\]]*]\\s*)?\\s*Caused by:\\s*(.+)$");
	private static final Pattern DIGITS = Pattern.compile("\\d+");

	private static final ConcurrentHashMap<String, ErrorEntry> entries = new ConcurrentHashMap<>();

	private static final Set<String> mutedFingerprints = ConcurrentHashMap.newKeySet();
	private static final List<MutePattern> mutePatterns = new ArrayList<>();

	private static boolean enabled = true;
	private static long minPostIntervalMs = 300_000L;
	private static int fingerprintFrames = 6;

	private ErrorManager() {}

	/** Loads persisted mute rules and tuning from the {@code errors} config. */
	public static synchronized void initialize() {
		var config = ConfigManager.getErrorsConfig();
		enabled = config.getBoolean("enabled");
		long interval = config.getLong("min-post-interval-ms");
		if(interval > 0) minPostIntervalMs = interval;
		int frames = config.getInt("fingerprint-frames");
		if(frames > 0) fingerprintFrames = frames;

		mutedFingerprints.clear();
		var muted = config.getList("muted-fingerprints");
		if(muted != null) mutedFingerprints.addAll(muted);

		mutePatterns.clear();
		var patterns = config.getList("mute-patterns");
		if(patterns != null) {
			for(String p : patterns) addPatternInternal(p, 0);
		}
		var thresholds = config.getList("pattern-thresholds");
		if(thresholds != null) {
			for(String entry : thresholds) {
				int sep = entry.lastIndexOf("|||");
				if(sep <= 0) continue;
				addPatternInternal(entry.substring(0, sep), parseIntSafe(entry.substring(sep + 3)));
			}
		}
	}

	// ---------------------------------------------------------------------
	// Capture entry points
	// ---------------------------------------------------------------------

	/** Reports a captured throwable (from the logback appender or logException). */
	public static void report(String context, Throwable throwable) {
		if(throwable == null) return;
		var full = new StringBuilder();
		if(context != null && !context.isBlank()) full.append(context).append('\n');
		appendThrowable(full, throwable);
		report(throwable.getClass().getName(), String.valueOf(throwable.getMessage()), full.toString());
	}

	/**
	 * Reports a pre-formatted error block (from the stdout/stderr stream tap).
	 *
	 * @param exceptionClass fully-qualified class, e.g. {@code java.io.FileNotFoundException}
	 * @param message        first-line message (may be null/empty)
	 * @param fullText       full assembled stack trace block
	 */
	public static void report(String exceptionClass, String message, String fullText) {
		try {
			if(!enabled) return;
			String fingerprint = fingerprint(exceptionClass, message, fullText);
			long now = System.currentTimeMillis();
			ErrorEntry entry = entries.computeIfAbsent(fingerprint,
					fp -> new ErrorEntry(fp, exceptionClass, safe(message), fullText, now));
			int count = entry.recordOccurrence(now);

			if(isMuted(entry)) return;
			int threshold = thresholdFor(entry);
			if(shouldPost(entry, count, threshold, now)) post(entry, count, now);
		} catch(Exception ignored) {
			// Never let error reporting throw back into the capture path.
		}
	}

	/** Reports a fatal/crash error: always posted, mentions staff, never muted. */
	public static void reportFatal(String context, Throwable throwable) {
		try {
			var embed = new EmbedBuilder()
					.setColor(0xB00020)
					.setTitle(":bangbang: [FATAL] " + (throwable != null ? throwable.getClass().getSimpleName() : "Server crash"));
			if(context != null && !context.isBlank()) embed.setDescription(limit(context, 2000));
			if(throwable != null) {
				var trace = new StringBuilder();
				appendThrowable(trace, throwable);
				embed.addField("Stack Trace", "```" + limit(trace.toString(), 1000) + "```", false);
			}
			var builder = new MessageCreateBuilder().setEmbeds(embed.build());
			Role staff = DiscordUtils.getStaffRole();
			if(staff != null) builder.setContent(staff.getAsMention() + " A fatal error has occurred on the server.");
			sendToLog(builder.build());
		} catch(Exception ignored) {
		}
	}

	// ---------------------------------------------------------------------
	// Posting decision
	// ---------------------------------------------------------------------

	private static boolean shouldPost(ErrorEntry entry, int count, int threshold, long now) {
		if(count < threshold) return false;
		if(count == threshold) return true; // first eligible alert
		return now - entry.getLastPostedAt() >= minPostIntervalMs; // periodic "still happening" update
	}

	private static void post(ErrorEntry entry, int count, long now) {
		// Bot may not be ready yet during early startup; skip building the message
		// (the occurrence is still counted and surfaces on the next report once ready).
		if(StarBridge.getBot() == null) return;
		entry.markPosted(now);
		var embed = new EmbedBuilder()
				.setColor(0xE67E22)
				.setTitle(":exclamation: " + entry.simpleClassName() + (count > 1 ? " (×" + count + ")" : ""));
		if(entry.getMessage() != null && !entry.getMessage().isBlank() && !"null".equals(entry.getMessage())) {
			embed.setDescription(limit(entry.getMessage(), 2000));
		}
		embed.addField("Class", entry.getExceptionClass(), false);
		embed.addField("Occurrences", String.valueOf(count), true);
		embed.addField("Fingerprint", "`" + entry.getFingerprint() + "`", true);
		embed.addField("Stack Trace", "```" + limit(entry.getFullText(), 1000) + "```", false);

		var fp = entry.getFingerprint();
		var message = new MessageCreateBuilder()
				.setEmbeds(embed.build())
				.addComponents(ActionRow.of(
						Button.danger(BUTTON_PREFIX + ":mute:" + fp, "Mute"),
						Button.secondary(BUTTON_PREFIX + ":pat:" + fp, "Mute pattern…"),
						Button.primary(BUTTON_PREFIX + ":stack:" + fp, "Show stack")))
				.build();
		sendToLog(message);
	}

	private static void sendToLog(MessageCreateData data) {
		var bot = StarBridge.getBot();
		if(bot != null) bot.sendToLogChannel(data);
	}

	// ---------------------------------------------------------------------
	// Mute rule queries + mutations (persisted)
	// ---------------------------------------------------------------------

	private static boolean isMuted(ErrorEntry entry) {
		if(mutedFingerprints.contains(entry.getFingerprint())) return true;
		String haystack = entry.getExceptionClass() + ": " + entry.getMessage();
		for(MutePattern mp : mutePatterns) {
			if(mp.threshold <= 0 && mp.matches(haystack, entry.getFullText())) return true;
		}
		return false;
	}

	private static int thresholdFor(ErrorEntry entry) {
		int threshold = 1;
		String haystack = entry.getExceptionClass() + ": " + entry.getMessage();
		for(MutePattern mp : mutePatterns) {
			if(mp.threshold > 0 && mp.matches(haystack, entry.getFullText())) threshold = Math.max(threshold, mp.threshold);
		}
		return threshold;
	}

	public static synchronized boolean muteFingerprint(String fingerprint) {
		boolean added = mutedFingerprints.add(fingerprint);
		if(added) persist();
		return added;
	}

	public static synchronized boolean unmute(String fingerprintOrPattern) {
		boolean changed = mutedFingerprints.remove(fingerprintOrPattern);
		changed |= mutePatterns.removeIf(mp -> mp.raw.equals(fingerprintOrPattern));
		if(changed) persist();
		return changed;
	}

	public static synchronized boolean mutePattern(String regex) {
		if(addPatternInternal(regex, 0)) {
			persist();
			return true;
		}
		return false;
	}

	public static synchronized boolean setThreshold(String regex, int threshold) {
		mutePatterns.removeIf(mp -> mp.raw.equals(regex));
		boolean added = addPatternInternal(regex, threshold);
		if(added) persist();
		return added;
	}

	private static boolean addPatternInternal(String regex, int threshold) {
		if(regex == null || regex.isBlank()) return false;
		for(MutePattern mp : mutePatterns) {
			if(mp.raw.equals(regex) && mp.threshold == threshold) return false;
		}
		try {
			mutePatterns.add(new MutePattern(regex, threshold));
			return true;
		} catch(PatternSyntaxException invalid) {
			return false;
		}
	}

	private static synchronized void persist() {
		FileConfiguration config = ConfigManager.getErrorsConfig();
		config.setList("muted-fingerprints", new ArrayList<>(mutedFingerprints));
		var patterns = new ArrayList<String>();
		var thresholds = new ArrayList<String>();
		for(MutePattern mp : mutePatterns) {
			if(mp.threshold > 0) thresholds.add(mp.raw + "|||" + mp.threshold);
			else patterns.add(mp.raw);
		}
		config.setList("mute-patterns", patterns);
		config.setList("pattern-thresholds", thresholds);
		config.saveConfig();
	}

	// ---------------------------------------------------------------------
	// Read-only views for the /errors command
	// ---------------------------------------------------------------------

	public static Collection<ErrorEntry> activeEntries() {
		return new ArrayList<>(entries.values());
	}

	public static ErrorEntry getEntry(String fingerprint) {
		return entries.get(fingerprint);
	}

	public static Set<String> getMutedFingerprints() {
		return new LinkedHashSet<>(mutedFingerprints);
	}

	public static List<String> getMutePatternDescriptions() {
		var out = new ArrayList<String>();
		for(MutePattern mp : mutePatterns) {
			out.add(mp.threshold > 0 ? mp.raw + " (threshold " + mp.threshold + ")" : mp.raw);
		}
		return out;
	}

	/** Builds the embed shown by {@code /errors list}. */
	public static net.dv8tion.jda.api.entities.MessageEmbed buildListEmbed() {
		var embed = new EmbedBuilder().setTitle(":bar_chart: Tracked Errors").setColor(0x3498DB);
		var active = activeEntries();
		if(active.isEmpty()) {
			embed.setDescription("No errors have been reported this session. :tada:");
		} else {
			var sb = new StringBuilder();
			active.stream()
					.sorted((a, b) -> Integer.compare(b.getCount(), a.getCount()))
					.limit(15)
					.forEach(e -> sb.append(mutedFingerprints.contains(e.getFingerprint()) ? "🔇 " : "• ")
							.append('`').append(e.getFingerprint()).append("` ")
							.append(e.simpleClassName()).append(" ×").append(e.getCount())
							.append(" (").append(relativeTime(e.getLastSeen())).append(")\n"));
			embed.setDescription(limit(sb.toString(), 4000));
		}
		var patterns = getMutePatternDescriptions();
		if(!patterns.isEmpty()) {
			embed.addField("Muted patterns", limit("`" + String.join("`\n`", patterns) + "`", 1000), false);
		}
		embed.setFooter(statsSummary());
		return embed.build();
	}

	public static String statsSummary() {
		int distinct = entries.size();
		long total = entries.values().stream().mapToLong(ErrorEntry::getCount).sum();
		return "Tracking **" + distinct + "** distinct error pattern(s), **" + total + "** total occurrence(s). "
				+ mutedFingerprints.size() + " muted fingerprint(s), " + mutePatterns.size() + " pattern rule(s).";
	}

	// ---------------------------------------------------------------------
	// Fingerprinting / parsing
	// ---------------------------------------------------------------------

	private static String fingerprint(String exceptionClass, String message, String fullText) {
		var sb = new StringBuilder(safe(exceptionClass)).append('\n');
		var frames = topFrames(fullText, fingerprintFrames);
		if(frames.isEmpty()) {
			// No stack frames — fall back to a digit-stripped message so varying
			// ids/paths still collapse together.
			sb.append(DIGITS.matcher(safe(message)).replaceAll("#"));
		} else {
			for(String f : frames) sb.append(f).append('\n');
		}
		return sha12(sb.toString());
	}

	private static List<String> topFrames(String fullText, int max) {
		var frames = new ArrayList<String>();
		if(fullText == null) return frames;
		for(String line : fullText.split("\n")) {
			var m = FRAME.matcher(line);
			if(m.find()) {
				String frame = m.group(1).trim();
				int paren = frame.indexOf('(');
				frames.add(paren > 0 ? frame.substring(0, paren) : frame); // drop (File.java:line)
				if(frames.size() >= max) break;
			}
		}
		return frames;
	}

	/** Parses a raw text block into (class, message); used by the stream tap. */
	public static String[] parseHeader(String line) {
		var m = HEADER.matcher(line.trim());
		if(m.matches()) return new String[]{m.group(1), m.group(2) == null ? "" : m.group(2).trim()};
		return null;
	}

	public static boolean isFrameLine(String line) {
		return FRAME.matcher(line).find() || CAUSED_BY.matcher(line).find()
				|| line.trim().startsWith("...") || line.trim().startsWith("Suppressed:");
	}

	private static void appendThrowable(StringBuilder sb, Throwable t) {
		sb.append(t.getClass().getName());
		if(t.getMessage() != null) sb.append(": ").append(t.getMessage());
		sb.append('\n');
		var trace = t.getStackTrace();
		for(int i = 0; i < trace.length && i < 20; i++) sb.append("\tat ").append(trace[i]).append('\n');
		if(t.getCause() != null && t.getCause() != t) {
			sb.append("Caused by: ");
			appendThrowable(sb, t.getCause());
		}
	}

	private static String sha12(String input) {
		try {
			var digest = MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8));
			var hex = new StringBuilder();
			for(int i = 0; i < 6; i++) hex.append(String.format("%02x", digest[i]));
			return hex.toString();
		} catch(NoSuchAlgorithmException e) {
			return Integer.toHexString(input.hashCode());
		}
	}

	private static int parseIntSafe(String s) {
		try {
			return Integer.parseInt(s.trim());
		} catch(NumberFormatException e) {
			return 0;
		}
	}

	private static String safe(String s) {
		return s == null ? "" : s;
	}

	private static String limit(String s, int max) {
		if(s == null) return "";
		return s.length() <= max ? s : s.substring(0, max - 1) + "…";
	}

	static String relativeTime(long epochMillis) {
		var d = Duration.between(Instant.ofEpochMilli(epochMillis), Instant.now());
		long sec = Math.max(0, d.getSeconds());
		if(sec < 60) return sec + "s ago";
		if(sec < 3600) return (sec / 60) + "m ago";
		if(sec < 86400) return (sec / 3600) + "h ago";
		return (sec / 86400) + "d ago";
	}

	/** A compiled mute/threshold rule. {@code threshold <= 0} means a hard mute. */
	private static final class MutePattern {
		final String raw;
		final int threshold;
		final Pattern compiled;

		MutePattern(String raw, int threshold) {
			this.raw = raw;
			this.threshold = threshold;
			this.compiled = Pattern.compile(raw, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
		}

		boolean matches(String header, String fullText) {
			return compiled.matcher(header).find() || (fullText != null && compiled.matcher(fullText).find());
		}
	}
}
