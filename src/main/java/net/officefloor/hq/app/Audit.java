package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
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

    private final Path file;

    private final DelegationRepository delegations;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, DelegationRepository delegations) {
        this.file = Path.of(path);
        this.delegations = delegations;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A change made by a user acting as a
     *  steward by delegation (in force by the sheet's today) ends with for=<the steward who delegated>; the ROLLUP
     *  and TOTALS lines do not. */
    public synchronized void record(String entry) {
        entry = entry + delegatedFor(entry);
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

    /** " for=<steward>" when the record's user (by=) acts by a delegation in force on its sheet (sheet=), else "". */
    private String delegatedFor(String entry) {
        String[] fields = entry.split(" ");
        if (fields[0].equals("ROLLUP") || fields[0].equals("TOTALS")) {
            return "";
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
            return "";
        }
        String steward = delegations.delegator(sheet, by);
        return steward == null ? "" : " for=" + steward;
    }

    /** Every record the file holds now, in file order (none when there is no file). */
    public synchronized List<String> lines() {
        try {
            if (!Files.exists(file)) {
                return List.of();
            }
            return Files.readAllLines(file, StandardCharsets.UTF_8).stream().filter(l -> !l.isEmpty()).toList();
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
