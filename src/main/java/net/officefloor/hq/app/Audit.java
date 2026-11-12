package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    /** Remove the audit file. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
