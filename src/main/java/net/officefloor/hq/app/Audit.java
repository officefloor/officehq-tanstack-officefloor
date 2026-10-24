package net.officefloor.hq.app;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import net.officefloor.hq.app.role.RoleRepository;
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

    /** The records that only report figures: they never carry the steward a delegate acts for. */
    private static final Set<String> FIGURES = Set.of("ROLLUP", "TOTALS", "ALERT");

    private final Path file;

    private final ObjectProvider<RoleRepository> roles;

    public Audit(@Value("${app.audit.file:.run/audit.log}") String path, ObjectProvider<RoleRepository> roles) {
        this.file = Path.of(path);
        this.roles = roles;
    }

    /** Append one audit record. Tests assert the exact line(s) in the file. A record of a change made by a user
     *  name while the sheet's today is inside a delegation of the steward role to it ends with for=&lt;the steward
     *  who delegated&gt;. */
    public void record(String entry) {
        write(entry + forDelegator(entry));
    }

    /** " for=&lt;steward&gt;" when the record's by= acts for a steward on its sheet= today, else "". */
    private String forDelegator(String entry) {
        int space = entry.indexOf(' ');
        if (space < 0 || FIGURES.contains(entry.substring(0, space))) {
            return "";
        }
        String sheet = field(entry, "sheet");
        String by = field(entry, "by");
        if (sheet == null || by == null || by.isEmpty()) {
            return "";
        }
        String delegator = roles.getObject().delegatorOf(sheet, by);
        return delegator == null ? "" : " for=" + delegator;
    }

    private static String field(String entry, String name) {
        String key = " " + name + "=";
        int start = entry.indexOf(key);
        if (start < 0) {
            return null;
        }
        start += key.length();
        int end = entry.indexOf(' ', start);
        return end < 0 ? entry.substring(start) : entry.substring(start, end);
    }

    private synchronized void write(String entry) {
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
