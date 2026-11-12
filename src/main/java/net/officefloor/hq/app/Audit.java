package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.role.DelegationStore;
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

    /** Lines that only report figures: never marked as made for a delegating steward. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;
    private final DelegationStore delegations;
    /** The records held back on this thread while a change that may yet be refused as a whole runs. */
    private final ThreadLocal<List<String>> held = new ThreadLocal<>();

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, DelegationStore delegations) {
        this.file = Path.of(path);
        this.delegations = delegations;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A change made by a user while a
     *  delegation of the steward role to them is in force ends with for=<the steward who delegated>. */
    public synchronized void record(String entry) {
        String delegator = delegator(entry);
        if (delegator != null) {
            entry = entry + " for=" + delegator;
        }
        List<String> holding = held.get();
        if (holding != null) {
            holding.add(entry);
            return;
        }
        append(entry);
    }

    /** Holds back the records this thread makes from now on, until {@link #release()} writes them or
     *  {@link #discard()} drops them: a change refused as a whole writes nothing. */
    public void hold() {
        held.set(new ArrayList<>());
    }

    /** Writes the records held back on this thread, in the order they were made, and stops holding. */
    public synchronized void release() {
        List<String> holding = held.get();
        held.remove();
        if (holding != null) {
            for (String entry : holding) {
                append(entry);
            }
        }
    }

    /** Drops the records held back on this thread, and stops holding. */
    public void discard() {
        held.remove();
    }

    private void append(String entry) {
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

    /** The steward who delegated to the line's by= user, when the line is a change and a delegation is in force. */
    private String delegator(String entry) {
        String[] fields = entry.split(" ");
        if (FIGURES.contains(fields[0])) {
            return null;
        }
        String sheet = null;
        String by = null;
        for (String f : fields) {
            if (sheet == null && f.startsWith("sheet=")) {
                sheet = f.substring("sheet=".length());
            } else if (by == null && f.startsWith("by=")) {
                by = f.substring("by=".length());
            }
        }
        return sheet == null || by == null ? null : delegations.delegator(sheet, by);
    }

    /** The sheet's lines exactly as the file holds them, newest first; with a person's name, only the lines
     *  carrying name=<it> or person=<it>, compared as names are (case and surrounding spaces ignored). */
    public synchronized List<String> lines(String sheet, String person) {
        if (!Files.exists(file)) {
            return List.of();
        }
        List<String> all;
        try {
            all = Files.readAllLines(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String marker = " sheet=" + sheet + " ";
        String name = person == null ? "" : person.trim().toLowerCase(java.util.Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (int i = all.size() - 1; i >= 0; i--) {
            String line = all.get(i);
            if ((line + " ").contains(marker) && (name.isEmpty() || about(line, name))) {
                found.add(line);
            }
        }
        return found;
    }

    /** A claim's timeline on the sheet, newest first, exactly as the file holds them: with a number, the lines carrying
     *  number=<it>; without, the lines carrying no number that name its person, amount and date together. */
    public synchronized List<String> timeline(String sheet, String number, String person, String amount, String date) {
        List<String> found = new ArrayList<>();
        String naming = " person=" + person + " amount=" + amount + " date=" + date + " ";
        for (String line : lines(sheet, null)) {
            String padded = line + " ";
            boolean ours = number != null
                    ? padded.contains(" number=" + number + " ")
                    : padded.contains(naming) && !padded.contains(" number=");
            if (ours) {
                found.add(line);
            }
        }
        return found;
    }

    /** The lines about the person on the sheet they were transferred from to {@code to}, newest first, exactly as
     *  the file holds them: from the latest record of their transfer to it, back (all of them when there is none). */
    public synchronized List<String> linesBefore(String from, String to, String person) {
        List<String> theirs = lines(from, person);
        String prefix = "PERSON_TRANSFERRED sheet=" + from + " ";
        for (int i = 0; i < theirs.size(); i++) {
            String line = theirs.get(i);
            if (line.startsWith(prefix) && (line + " ").contains(" to=" + to + " ")) {
                return theirs.subList(i, theirs.size());
            }
        }
        return theirs;
    }

    /** Whether the line carries name=<name> or person=<name>: the value runs to the line's end or to the next field. */
    private static boolean about(String line, String name) {
        String lower = line.toLowerCase(java.util.Locale.ROOT);
        for (String key : List.of(" name=", " person=")) {
            int at = lower.indexOf(key);
            while (at >= 0) {
                int start = at + key.length();
                if (lower.startsWith(name, start)) {
                    String rest = lower.substring(start + name.length());
                    if (rest.isEmpty() || rest.matches(" [a-z][a-z0-9-]*=.*")) {
                        return true;
                    }
                }
                at = lower.indexOf(key, at + 1);
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
