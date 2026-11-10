package net.officefloor.hq.app.sheet;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the archive. Runs after every rule has passed, so a refused archive writes nothing. */
public class AuditSheetArchived {
    public void audit(@Val Sheet sheet, HttpServletRequest request, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.id() + " by=" + Audit.user(request));
    }
}
