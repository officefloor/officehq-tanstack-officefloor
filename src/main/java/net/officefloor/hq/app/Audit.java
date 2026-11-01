package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.Set;
import net.officefloor.hq.app.delegation.Delegations;
import net.officefloor.hq.app.sheet.SheetClock;
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

    /** The lines that only report figures, so never carry whom a delegated steward acts for. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;
    private final Delegations delegations;
    private final SheetClock clock;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, Delegations delegations, SheetClock clock) {
        this.file = Path.of(path);
        this.delegations = delegations;
        this.clock = clock;
    }

    /**
     * Append one audit record. Tests assert the exact line(s) in the file. A change made by a user lent the steward
     * role on the sheet's today ends with for=<the steward who delegated>.
     */
    public synchronized void record(String entry) {
        entry = entry + actingFor(entry).map(s -> " for=" + s).orElse("");
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

    /** The steward the line's user acts for, read from its leading "EVENT sheet=<id> by=<user>". */
    private Optional<String> actingFor(String entry) {
        String[] parts = entry.split(" ", 4);
        if (parts.length < 3 || FIGURES.contains(parts[0]) || !parts[1].startsWith("sheet=")
                || !parts[2].startsWith("by=")) {
            return Optional.empty();
        }
        String sheet = parts[1].substring("sheet=".length());
        return delegations.delegatorOn(sheet, parts[2].substring("by=".length()), clock.today(sheet));
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
