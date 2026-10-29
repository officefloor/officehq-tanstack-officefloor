package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import net.officefloor.server.http.HttpHeader;
import net.officefloor.server.http.ServerHttpConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Audit sink: appends one record per line to a KNOWN FILE (app.audit.file, default .run/audit.log)
 * so tests can assert side-effects by reading that file — the second test-assertion channel
 * alongside the UI. The file path + one-record-per-line format is a stable contract, like
 * data-testid: features may be rewritten as long as they keep emitting the agreed records.
 *
 * Inject this bean into any feature that must record an audit entry. Each write flushes (append +
 * SYNC) so a test reads the record immediately.
 */
@Service
public class Audit {

    /** The lines that only report figures: they never name the steward a change is made for. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;
    private final ObjectProvider<Settings> settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, ObjectProvider<Settings> settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /**
     * Append one audit record. Tests assert the exact line(s) in the file. A change made by a user the steward role is
     * delegated to (for the sheet's today) ends with for=<the steward who delegated>, unless the line only reports figures.
     */
    public void record(String entry) {
        record(entry, null);
    }

    /**
     * Append one audit record about the sheet as it stood on {@code on} (null: the sheet's today): a line by a user the
     * steward role was delegated to for that date ends with for=<the steward who delegated>, as {@link #record(String)}.
     */
    public synchronized void record(String entry, java.time.LocalDate on) {
        String delegator = delegator(entry, on);
        if (delegator != null) {
            entry = entry + " for=" + delegator;
        }
        try {
            Path parent = file.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(file, entry + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.SYNC);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** The steward who delegated to the user the line is by, on the line's sheet; null when none did or the line only reports figures. */
    private String delegator(String entry, java.time.LocalDate on) {
        String[] parts = entry.split(" ", 4);
        if (parts.length < 3 || FIGURES.contains(parts[0]) || !parts[1].startsWith("sheet=") || !parts[2].startsWith("by=")) {
            return null;
        }
        String sheetId = parts[1].substring("sheet=".length());
        String user = parts[2].substring("by=".length());
        return on == null ? settings.getObject().delegator(sheetId, user) : settings.getObject().delegator(sheetId, user, on);
    }

    /** The signed-in user a write names in its X-User header ("" when it names none). */
    public static String user(ServerHttpConnection connection) {
        HttpHeader header = connection.getRequest().getHeaders().getHeader("X-User");
        return header == null ? "" : header.getValue();
    }

    /** One record: the event, then sheet=<id> and by=<user>, then the fields, separated by single spaces. */
    public static String line(String event, String sheetId, String user, String... fields) {
        StringBuilder line = new StringBuilder(event).append(" sheet=").append(sheetId)
                .append(" by=").append(user == null ? "" : user.trim());
        for (String field : fields) {
            line.append(' ').append(field);
        }
        return line.toString();
    }

    /**
     * The sheet's lines exactly as the file holds them, newest first. With a name (not blank), only the lines about that
     * person: those carrying name=<the name> or person=<the name>, compared as names are (case and surrounding spaces ignored).
     */
    public synchronized List<String> lines(String sheetId, String name) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> all;
        try {
            all = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        List<String> lines = new ArrayList<>();
        for (String line : all) {
            String[] parts = line.split(" ", 3);
            if (parts.length >= 2 && parts[1].equals("sheet=" + sheetId)
                    && (wanted.isEmpty() || names(line).contains(wanted))) {
                lines.add(line);
            }
        }
        Collections.reverse(lines);
        return lines;
    }

    /**
     * The sheet's lines about one claim, exactly as the file holds them, newest first: those carrying number=<its
     * number>, and those carrying no number (a numbered line is about the numbered claim it names) whose person=
     * (compared as names are), amount= and date= are all the claim's (as it was before it was numbered on settling).
     */
    public synchronized List<String> claimLines(String sheetId, String number, String person, String amount, String date) {
        String wanted = person == null ? "" : person.trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (String line : lines(sheetId, null)) {
            java.util.Map<String, List<String>> fields = fields(line);
            boolean about = number != null && !number.isBlank()
                    && fields.getOrDefault("number", List.of()).contains(number)
                    || !fields.containsKey("number") && fields.getOrDefault("person", List.of()).stream()
                            .anyMatch(n -> n.trim().toLowerCase(Locale.ROOT).equals(wanted))
                            && fields.getOrDefault("amount", List.of()).contains(amount)
                            && fields.getOrDefault("date", List.of()).contains(date);
            if (about) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** A line's fields after its event, by key; a value runs up to the next field. */
    private static java.util.Map<String, List<String>> fields(String line) {
        java.util.Map<String, List<String>> fields = new java.util.HashMap<>();
        Matcher m = FIELD.matcher(line);
        while (m.find()) {
            fields.computeIfAbsent(m.group(1), k -> new ArrayList<>()).add(m.group(2));
        }
        return fields;
    }

    private static final Pattern FIELD = Pattern.compile(" ([A-Za-z][\\w-]*)=(.*?)(?= [A-Za-z][\\w-]*=|$)");

    /** The values of a line's name= and person= fields, trimmed and lower-cased; a value runs up to the next field. */
    private static List<String> names(String line) {
        List<String> names = new ArrayList<>();
        Matcher m = NAME_FIELD.matcher(line);
        while (m.find()) {
            names.add(m.group(1).trim().toLowerCase(Locale.ROOT));
        }
        return names;
    }

    private static final Pattern NAME_FIELD = Pattern.compile(" (?:name|person)=(.*?)(?= [A-Za-z][\\w-]*=|$)");

    /**
     * The sheet's activity: for every user (by=) and every event the sheet's lines hold, one line
     * {@code <user> <EVENT> <count>}, sorted by user then event.
     */
    public synchronized List<String> activity(String sheetId) {
        java.util.TreeMap<String, java.util.TreeMap<String, Integer>> counts = new java.util.TreeMap<>();
        for (String line : lines(sheetId, null)) {
            String[] parts = line.split(" ", 4);
            String user = parts.length >= 3 && parts[2].startsWith("by=") ? parts[2].substring("by=".length()) : "";
            counts.computeIfAbsent(user, k -> new java.util.TreeMap<>()).merge(parts[0], 1, Integer::sum);
        }
        List<String> activity = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> activity.add(user + " " + event + " " + n)));
        return activity;
    }

    /** Remove the audit file. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
