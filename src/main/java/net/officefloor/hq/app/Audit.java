package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

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

    /** The lines that only report figures: they never carry for=. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;

    private final ObjectProvider<Settings> settings;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, ObjectProvider<Settings> settings) {
        this.file = Path.of(path);
        this.settings = settings;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. */
    public void record(String entry) {
        append(delegated(entry));
    }

    /**
     * The entry, ending with for=<the steward who delegated> when its by= user acts as a steward by a
     * delegation covering the sheet's today — unless it is a line that only reports figures.
     */
    private String delegated(String entry) {
        String[] fields = entry.split(" ");
        if (FIGURES.contains(fields[0])) {
            return entry;
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
            return entry;
        }
        String delegator = settings.getObject().delegator(sheet, by);
        return delegator == null ? entry : entry + " for=" + delegator;
    }

    private synchronized void append(String entry) {
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
     * Append one audit record once the current transaction commits, so a change that is rolled back
     * (refused by a later step) writes no line. Outside a transaction it is appended at once.
     */
    public void recordOnCommit(String raw) {
        String entry = delegated(raw);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    append(entry);
                }
            });
        } else {
            append(entry);
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
