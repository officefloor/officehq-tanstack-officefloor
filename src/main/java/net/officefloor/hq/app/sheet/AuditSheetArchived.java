package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the sheet just archived. */
public class AuditSheetArchived {
    public void audit(@Val Sheet sheet, @RequestParam("user") String user, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.id() + " by=" + user);
    }
}
