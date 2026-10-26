package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** This sheet's record of the clone; then the clone's first record, followed by its rollup lines and its totals. */
public class RecordSheetCloned {
    public void record(@Val SheetResponse sheet, @Val CloneInput input, @Val RollupBefore before, @Val ChangedBy by,
            Audit audit, Rollup rollup) {
        audit.record("SHEET_CLONE_MADE sheet=" + sheet.id() + " by=" + by.name() + " to=" + input.to());
        audit.record("SHEET_CLONED sheet=" + input.to() + " by=" + by.name() + " from=" + sheet.id());
        rollup.record(input.to(), before.snapshot(), by, true);
    }
}
