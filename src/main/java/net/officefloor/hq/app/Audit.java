package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
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

    /** Remove the audit file. */
    public synchronized void clear() {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
