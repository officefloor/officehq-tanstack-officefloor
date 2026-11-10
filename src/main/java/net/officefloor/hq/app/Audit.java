package net.officefloor.hq.app;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    /** Remove the audit file. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
