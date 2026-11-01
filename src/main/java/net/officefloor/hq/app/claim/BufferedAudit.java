package net.officefloor.hq.app.claim;

import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.settings.Settings;

/** Holds the lines of a change that may yet be refused as a whole, writing them to the audit file
 *  only once the whole change has gone through: a refused change writes nothing. */
class BufferedAudit extends Audit {
    private final List<String> lines = new ArrayList<>();

    BufferedAudit(Settings settings) {
        super("", settings);
    }

    @Override
    public synchronized void record(String entry) {
        lines.add(entry);
    }

    /** Write the held lines, in order, to the audit file. */
    void flush(Audit audit) {
        lines.forEach(audit::record);
        lines.clear();
    }
}
