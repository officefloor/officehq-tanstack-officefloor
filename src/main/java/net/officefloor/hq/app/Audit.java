package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import net.officefloor.hq.app.setting.RoleRepository;
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

    /** The lines that only report figures: they never name a delegating steward. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;

    private final RoleRepository roles;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, RoleRepository roles) {
        this.file = Path.of(path);
        this.roles = roles;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A change made by a user
     *  acting as a steward by delegation (the sheet's today inside its range) ends with
     *  {@code for=<the steward who delegated>}. */
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

    /** " for=<steward>" when the line's user acts by delegation on its sheet; else "". */
    private String delegatedFor(String entry) {
        String[] fields = entry.split(" ");
        if (FIGURES.contains(fields[0])) return "";
        String sheet = null;
        String by = null;
        for (String field : fields) {
            if (sheet == null && field.startsWith("sheet=")) sheet = field.substring("sheet=".length());
            else if (by == null && field.startsWith("by=")) by = field.substring("by=".length());
        }
        if (sheet == null || by == null || by.isEmpty()) return "";
        String steward = roles.delegatorOf(sheet, by);
        return steward == null ? "" : " for=" + steward;
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
