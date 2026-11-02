package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import net.officefloor.hq.app.settings.SettingsRepository;
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

    /** The lines that only report figures (or tell of a change already recorded): never marked for a delegator. */
    private static final Set<String> REPORTS = Set.of("ROLLUP", "TOTALS", "ALERT", "NOTIFY");

    private final Path file;

    private final ObjectProvider<SettingsRepository> settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, ObjectProvider<SettingsRepository> settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A change made by a user acting
     *  under a delegation of the steward role (one covering the sheet's today) ends with for=<the steward
     *  who delegated>. */
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

    /** " for=<steward>" when the record (EVENT sheet=<id> by=<user> ...) is a change by a delegated user. */
    private String delegatedFor(String entry) {
        String[] parts = entry.split(" ", 4);
        if (parts.length < 3 || REPORTS.contains(parts[0]) || !parts[1].startsWith("sheet=") || !parts[2].startsWith("by=")) {
            return "";
        }
        String delegator = settings.getObject().delegator(parts[1].substring("sheet=".length()), parts[2].substring("by=".length()));
        return delegator == null ? "" : " for=" + delegator;
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
