package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only an archive that went through is recorded. */
public class RecordSheetArchived {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Sheet sheet, Audit audit) {
        audit.record("SHEET_ARCHIVED sheet=" + sheet.id() + " by=" + (by == null ? "" : by));
    }
}
