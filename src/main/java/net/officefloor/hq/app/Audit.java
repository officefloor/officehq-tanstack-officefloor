package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.settings.Settings;
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
 * A change made by a user acting as a steward by delegation (the sheet's today inside the range)
 * ends each of its lines with for=<the steward who delegated> (a SNAPSHOT among them: it is written
 * by the today change the user makes); the lines that only report figures (ROLLUP, TOTALS, ALERT) do not.
 */
@Service
public class Audit {

    /** The events that only report figures: never marked for a delegating steward. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private static final Pattern SHEET_BY = Pattern.compile("^(\\S+) sheet=(\\S+) by=(\\S+)");

    private final Path file;
    private final Settings settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, Settings settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. */
    public synchronized void record(String entry) {
        Matcher m = SHEET_BY.matcher(entry);
        if (m.find() && !FIGURES.contains(m.group(1))) {
            String delegator = settings.delegator(m.group(2), m.group(3));
            if (delegator != null) {
                entry = entry + " for=" + delegator;
            }
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

    /**
     * The sheet's lines exactly as the file holds them, newest first. With a name given, only the
     * lines carrying name=<the name> or person=<the name>, compared as names are (case and the
     * spaces around it ignored). Reads the file; records nothing.
     */
    public synchronized List<String> linesFor(String sheet, String name) {
        List<String> all;
        try {
            all = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String key = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> lines = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            String line = all.get(i);
            Matcher m = SHEET_BY.matcher(line);
            if (!m.find() || !m.group(2).equals(sheet)) {
                continue;
            }
            if (key.isEmpty() || about(line, key)) {
                lines.add(line);
            }
        }
        return lines;
    }

    /** Whether the line's name= or person= field holds this (lower-cased, trimmed) name. */
    private static boolean about(String line, String key) {
        String[] tokens = line.split(" ");
        for (int i = 0; i < tokens.length; i++) {
            String value;
            if (tokens[i].startsWith("name=")) {
                value = tokens[i].substring("name=".length());
            } else if (tokens[i].startsWith("person=")) {
                value = tokens[i].substring("person=".length());
            } else {
                continue;
            }
            // A name with spaces in it runs on until the next field (a token carrying '=').
            StringBuilder full = new StringBuilder(value);
            for (int j = i + 1; j < tokens.length && !tokens[j].contains("="); j++) {
                full.append(' ').append(tokens[j]);
            }
            if (full.toString().trim().toLowerCase(Locale.ROOT).equals(key)) {
                return true;
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
