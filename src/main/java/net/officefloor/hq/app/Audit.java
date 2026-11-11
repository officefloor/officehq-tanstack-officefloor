package net.officefloor.hq.app;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.delegation.DelegationRepository;
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

    /** The lines that only report figures a change moved: they are never marked as made for a delegator. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    /** A record's head: its event name, then {@code sheet=<id> by=<user>}, the user up to the next field. */
    private static final Pattern HEAD = Pattern.compile("^(\\S+) sheet=(\\S+) by=(.*?)(?= [a-z][a-z-]*=|$)");

    private final Path file;
    private final DelegationRepository delegations;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, DelegationRepository delegations) {
        this.file = Path.of(path);
        this.delegations = delegations;
    }

    /**
     * Append one audit record. Tests assert the exact line(s) in the file. A change made by a user acting
     * as a steward by a delegation, for the sheet's today, ends with {@code for=<the steward who delegated>}.
     */
    public synchronized void record(String entry) {
        Matcher head = HEAD.matcher(entry);
        if (head.find() && !FIGURES.contains(head.group(1))) {
            String delegator = delegations.delegatorOf(head.group(2), head.group(3));
            if (delegator != null) entry = entry + " for=" + delegator;
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
     * The signed-in user who made a change, as the screen sends it in the {@code X-User} header
     * (URI-encoded, so any name fits a header).
     */
    public static String user(HttpServletRequest request) {
        String header = request.getHeader("X-User");
        return header == null ? "" : URLDecoder.decode(header, StandardCharsets.UTF_8);
    }

    /** A person field of a record: {@code name=} or {@code person=}, its value up to the next field. */
    private static final Pattern PERSON = Pattern.compile("(?<= )(?:name|person)=(.*?)(?= [a-z][a-z-]*=|$)");

    /**
     * The sheet's records as the file holds them, in file order. A non-blank {@code name} keeps only
     * the records carrying {@code name=} or {@code person=} that name, case and surrounding spaces ignored.
     */
    public synchronized List<String> linesFor(String sheetId, String name) {
        List<String> all;
        try {
            all = Files.exists(file) ? Files.readAllLines(file, StandardCharsets.UTF_8) : List.of();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return all.stream().filter(line -> {
            Matcher head = HEAD.matcher(line);
            if (!head.find() || !head.group(2).equals(sheetId)) return false;
            if (wanted.isEmpty()) return true;
            Matcher field = PERSON.matcher(line);
            while (field.find()) {
                if (field.group(1).trim().toLowerCase(Locale.ROOT).equals(wanted)) return true;
            }
            return false;
        }).toList();
    }

    /** A field of a record: its key, then its value up to the next field. */
    private static final Pattern FIELD = Pattern.compile("(?<= )([a-z][a-z-]*)=(.*?)(?= [a-z][a-z-]*=|$)");

    /**
     * A claim's records as the file holds them, in file order: with a number, the sheet's records
     * carrying {@code number=} that number; without, those carrying no number that name its
     * {@code person=}, {@code amount=} and {@code date=} together.
     */
    public synchronized List<String> claimLinesFor(String sheetId, String number, String person,
            String amount, String date) {
        return linesFor(sheetId, "").stream().filter(line -> {
            java.util.Map<String, String> fields = new java.util.HashMap<>();
            Matcher field = FIELD.matcher(line.substring(line.indexOf(" by=") + 1));
            while (field.find()) fields.putIfAbsent(field.group(1), field.group(2));
            if (number != null) return number.equals(fields.get("number"));
            return !fields.containsKey("number") && person.equals(fields.get("person"))
                    && amount.equals(fields.get("amount")) && date.equals(fields.get("date"));
        }).toList();
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
