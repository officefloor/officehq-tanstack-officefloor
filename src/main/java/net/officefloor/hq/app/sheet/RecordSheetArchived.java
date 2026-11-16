package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes SHEET_ARCHIVED to the audit file once the sheet is archived. */
public class RecordSheetArchived {
    public void record(@Val Sheet sheet, @Val ArchiveRequest request, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.id() + " by=" + request.by());
    }
}
