package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.setting.SettingRepository;
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
 *
 * A change made by a user acting as a steward by delegation (one in force on the sheet's today) ends with
 * {@code for=<the steward who delegated>}; lines that only report figures (ROLLUP, TOTALS, ALERT) do not. A SNAPSHOT,
 * written as the delegated user moves the sheet's today (before it changes), is marked like a change.
 */
@Service
public class Audit {

    /** The events that only report figures: never marked for a delegator. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    /** A record's event, sheet and user: "EVENT sheet=<id> by=<user> ...". */
    private static final Pattern HEAD = Pattern.compile("^(\\S+) sheet=(\\S+) by=(\\S+)");

    private final Path file;

    private final SettingRepository settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, SettingRepository settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. */
    public synchronized void record(String entry) {
        write(entry + forDelegator(entry));
    }

    /** " for=<steward>" when the record is a change by a user acting as a steward by delegation, else "". */
    private String forDelegator(String entry) {
        Matcher head = HEAD.matcher(entry);
        if (!head.find() || FIGURES.contains(head.group(1))) {
            return "";
        }
        String by = settings.delegatorOf(head.group(2), head.group(3));
        return by == null ? "" : " for=" + by;
    }

    private void write(String entry) {
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

    /**
     * The sheet's lines exactly as the file holds them, newest first. With a name (not blank), only the lines about
     * that person: those carrying {@code name=<name>} or {@code person=<name>}, compared as names are (case and
     * surrounding spaces ignored). Reads the file; keeps no record of its own.
     */
    public synchronized List<String> lines(String sheetId, String name) {
        String wanted = name == null ? "" : name.strip().toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        try {
            if (!Files.exists(file)) {
                return found;
            }
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                Matcher head = HEAD.matcher(line);
                if (head.find() && head.group(2).equals(sheetId) && (wanted.isEmpty() || isAbout(line, wanted))) {
                    found.add(line);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Collections.reverse(found);
        return found;
    }

    /** Whether the line carries name=<wanted> or person=<wanted> (wanted is lower case), the value ending the line or followed by a space. */
    private static boolean isAbout(String line, String wanted) {
        String lower = line.toLowerCase(Locale.ROOT);
        for (String key : List.of(" name=", " person=")) {
            String field = key + wanted;
            for (int at = lower.indexOf(field); at >= 0; at = lower.indexOf(field, at + 1)) {
                int end = at + field.length();
                if (end == lower.length() || lower.charAt(end) == ' ') {
                    return true;
                }
            }
        }
        return false;
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
