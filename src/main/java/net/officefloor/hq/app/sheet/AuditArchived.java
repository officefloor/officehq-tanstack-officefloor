package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records SHEET_ARCHIVED sheet=<id> by=<user>. */
public class AuditArchived {
    public void audit(@PathVariable("sheetId") String sheetId, @Val ArchiveRequest request, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheetId.trim() + " by=" + request.user());
    }
}
