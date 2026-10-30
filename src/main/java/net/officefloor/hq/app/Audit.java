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
import net.officefloor.hq.app.setting.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    /** The lines that only report figures: they never carry for=. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;

    private final ObjectProvider<Settings> settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, ObjectProvider<Settings> settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. */
    public void record(String entry) {
        append(delegated(entry));
    }

    /**
     * The entry, ending with for=<the steward who delegated> when its by= user acts as a steward by a
     * delegation covering the sheet's today — unless it is a line that only reports figures.
     */
    private String delegated(String entry) {
        String[] fields = entry.split(" ");
        if (FIGURES.contains(fields[0])) {
            return entry;
        }
        String sheet = null;
        String by = null;
        for (String field : fields) {
            if (sheet == null && field.startsWith("sheet=")) {
                sheet = field.substring("sheet=".length());
            } else if (by == null && field.startsWith("by=")) {
                by = field.substring("by=".length());
            }
        }
        if (sheet == null || by == null) {
            return entry;
        }
        String delegator = settings.getObject().delegator(sheet, by);
        return delegator == null ? entry : entry + " for=" + delegator;
    }

    private synchronized void append(String entry) {
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
     * Append one audit record once the current transaction commits, so a change that is rolled back
     * (refused by a later step) writes no line. Outside a transaction it is appended at once.
     */
    public void recordOnCommit(String raw) {
        String entry = delegated(raw);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    append(entry);
                }
            });
        } else {
            append(entry);
        }
    }

    /**
     * The sheet's lines exactly as the file holds them, newest first. With a name (not blank), only
     * the lines carrying name=<it> or person=<it>, compared as names are (case and surrounding
     * spaces ignored). Read from the file: the viewer is not a second record.
     */
    public synchronized List<String> lines(String sheet, String name) {
        String wanted = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        if (!Files.exists(file)) {
            return found;
        }
        List<String> all;
        try {
            all = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (String line : all) {
            List<String[]> fields = fields(line);
            if (!has(fields, "sheet", sheet, false)) {
                continue;
            }
            if (!wanted.isEmpty() && !has(fields, "name", wanted, true) && !has(fields, "person", wanted, true)) {
                continue;
            }
            found.add(line);
        }
        Collections.reverse(found);
        return found;
    }

    /** Whether a field key=value is among the line's fields (a name compared trimmed, case ignored). */
    private static boolean has(List<String[]> fields, String key, String value, boolean asName) {
        for (String[] field : fields) {
            if (field[0].equals(key)) {
                String v = asName ? field[1].trim().toLowerCase(Locale.ROOT) : field[1];
                if (v.equals(value)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** A line's key=value fields; a word without key= continues the value before it (a name with spaces). */
    private static List<String[]> fields(String line) {
        List<String[]> fields = new ArrayList<>();
        String[] words = line.split(" ");
        for (int i = 1; i < words.length; i++) {
            String word = words[i];
            int eq = word.indexOf('=');
            if (eq > 0 && word.substring(0, eq).matches("[a-z][a-z-]*")) {
                fields.add(new String[] { word.substring(0, eq), word.substring(eq + 1) });
            } else if (!fields.isEmpty()) {
                String[] last = fields.get(fields.size() - 1);
                last[1] = last[1] + " " + word;
            }
        }
        return fields;
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
