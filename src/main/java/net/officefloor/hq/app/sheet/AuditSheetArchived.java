package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the sheet archived, once it is saved. */
public class AuditSheetArchived {
    public void audit(@Val Sheet sheet, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.getId() + " by=" + AuditUser.decode(user));
    }
}
