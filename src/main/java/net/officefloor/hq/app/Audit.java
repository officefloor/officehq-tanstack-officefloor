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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.officefloor.hq.app.access.DelegationRepository;
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

    /** The records that only report figures; they never carry who a delegate acts for. */
    private static final Set<String> REPORTS = Set.of("ROLLUP", "TOTALS", "ALERT");

    private static final Pattern HEAD = Pattern.compile("^(\\S+) sheet=(\\S+) by=(\\S+)");

    private final Path file;

    private final DelegationRepository delegations;

    /** The records held back on this thread while a change may yet be undone; null when records are written at once. */
    private final ThreadLocal<List<String>> held = new ThreadLocal<>();

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, DelegationRepository delegations) {
        this.file = Path.of(path);
        this.delegations = delegations;
    }

    /**
     * Append one audit record. Tests assert the exact line(s) in the file. A change made by a user acting as a
     * steward by a delegation on the sheet's today ends with for=<the steward who delegated>.
     */
    public synchronized void record(String entry) {
        Matcher head = HEAD.matcher(entry);
        if (head.find() && !REPORTS.contains(head.group(1))) {
            entry += delegations.actingFor(head.group(2), head.group(3)).map(steward -> " for=" + steward).orElse("");
        }
        List<String> holding = held.get();
        if (holding != null) {
            holding.add(entry);
            return;
        }
        write(entry);
    }

    /** Hold back the records this thread makes from now on, until {@link #release(boolean)}. */
    public void hold() {
        held.set(new ArrayList<>());
    }

    /** Stop holding back this thread's records: write the held ones in order, or drop them all. */
    public synchronized void release(boolean write) {
        List<String> holding = held.get();
        held.remove();
        if (write && holding != null) {
            holding.forEach(this::write);
        }
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

    /** Remove the audit file. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
