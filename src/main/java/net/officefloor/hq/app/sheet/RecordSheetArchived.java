package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordSheetArchived {
    public void record(@Val SheetResponse sheet, @Val ChangedBy by, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.id() + " by=" + by.name());
    }
}
