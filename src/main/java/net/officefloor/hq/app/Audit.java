package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.role.DelegationRepository;
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

    /** Lines that only report figures: never marked as made for a delegating steward. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");
    private static final Pattern HEAD = Pattern.compile("^(\\S+) sheet=(\\S+) by=(\\S+)");
    /** A name= or person= field: its value runs to the next field (a space then key=) or the end of the line. */
    private static final Pattern ABOUT = Pattern.compile(" (?:name|person)=(.*?)(?= [^\\s=]+=|$)");

    /** Any field: its key, and its value running to the next field (a space then key=) or the end of the line. */
    private static final Pattern FIELD = Pattern.compile(" ([^\\s=]+)=(.*?)(?= [^\\s=]+=|$)");

    private final Path file;
    private final ObjectProvider<DelegationRepository> delegations;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path,
            ObjectProvider<DelegationRepository> delegations) {
        this.file = Path.of(path);
        this.delegations = delegations;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A change made by a user acting as a
     *  steward by delegation (the sheet's today inside its range) ends with {@code for=<the delegating steward>}. */
    public synchronized void record(String entry) {
        Matcher m = HEAD.matcher(entry);
        if (m.find() && !FIGURES.contains(m.group(1))) {
            String steward = delegations.getObject().actingFor(m.group(2), m.group(3)).orElse(null);
            if (steward != null) {
                entry = entry + " for=" + steward;
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

    /** The sheet's lines exactly as the file holds them, newest first. With a name (blank: every line), only the
     *  lines carrying name=<it> or person=<it>, compared as names are (case and surrounding spaces ignored). */
    public synchronized List<String> lines(String sheetId, String name) {
        String key = name.trim().toLowerCase(Locale.ROOT);
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
            Matcher head = HEAD.matcher(line);
            if (!head.find() || !head.group(2).equals(sheetId)) {
                continue;
            }
            if (key.isEmpty() || about(line, key)) {
                found.add(line);
            }
        }
        Collections.reverse(found);
        return found;
    }

    /** The sheet's activity: one line {@code <user> <EVENT> <count>} for every user (by=) and event in the sheet's
     *  lines, sorted by user then event. */
    public synchronized List<String> activity(String sheetId) {
        java.util.TreeMap<String, java.util.TreeMap<String, Integer>> counts = new java.util.TreeMap<>();
        for (String line : lines(sheetId, "")) {
            Matcher head = HEAD.matcher(line);
            if (head.find()) {
                counts.computeIfAbsent(head.group(3), (u) -> new java.util.TreeMap<>()).merge(head.group(1), 1,
                        Integer::sum);
            }
        }
        List<String> found = new ArrayList<>();
        counts.forEach((user, events) -> events.forEach((event, n) -> found.add(user + " " + event + " " + n)));
        return found;
    }

    /** The lines about the person (see #lines) on the sheet they were transferred from to {@code to}, newest first,
     *  from the last PERSON_TRANSFERRED of them to that sheet back: what the sheet recorded about them while they were there. */
    public synchronized List<String> history(String fromSheet, String name, String to) {
        List<String> found = lines(fromSheet, name);
        for (int i = 0; i < found.size(); i++) {
            String line = found.get(i);
            Matcher head = HEAD.matcher(line);
            if (head.find() && head.group(1).equals("PERSON_TRANSFERRED")
                    && (line + " ").contains(" to=" + to + " ")) {
                return found.subList(i, found.size());
            }
        }
        return found;
    }

    private static boolean about(String line, String key) {
        Matcher m = ABOUT.matcher(line);
        while (m.find()) {
            if (m.group(1).trim().toLowerCase(Locale.ROOT).equals(key)) {
                return true;
            }
        }
        return false;
    }

    /** The claim's timeline: the sheet's lines about one claim exactly as the file holds them, newest first. For a claim
     *  with a number, the lines carrying number=<it>; for one without, the lines carrying no number that name its person
     *  (compared as names are), amount and date together. */
    public synchronized List<String> claimLines(String sheetId, String number, String person, BigDecimal amount,
            LocalDate date) {
        String key = person.trim().toLowerCase(Locale.ROOT);
        List<String> found = new ArrayList<>();
        for (String line : lines(sheetId, "")) {
            List<String> numbers = values(line, "number");
            if (number != null) {
                if (numbers.contains(number)) {
                    found.add(line);
                }
                continue;
            }
            if (!numbers.isEmpty()) {
                continue;
            }
            boolean named = values(line, "person").stream()
                    .anyMatch((v) -> v.trim().toLowerCase(Locale.ROOT).equals(key));
            boolean dated = values(line, "date").contains(date.toString());
            boolean amounted = values(line, "amount").stream().anyMatch((v) -> {
                try {
                    return new BigDecimal(v).compareTo(amount) == 0;
                } catch (NumberFormatException e) {
                    return false;
                }
            });
            if (named && dated && amounted) {
                found.add(line);
            }
        }
        return found;
    }

    /** The values of the line's fields named {@code key}; each runs to the next field or the end of the line. */
    private static List<String> values(String line, String key) {
        List<String> found = new ArrayList<>();
        Matcher m = FIELD.matcher(line);
        while (m.find()) {
            if (m.group(1).equals(key)) {
                found.add(m.group(2));
            }
        }
        return found;
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
