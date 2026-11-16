package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;

/** Writes SHEET_CLONE_MADE on the sheet cloned, then the clone's first lines: SHEET_CLONED, followed by ROLLUP for
 *  every person and TOTALS. */
public class RecordSheetCloned {
    public void record(@Val SheetClone clone, @Val CloneRequest request, Audit audit, Rollup rollup) {
        audit.record("SHEET_CLONE_MADE sheet=" + clone.from() + " by=" + request.by() + " to=" + clone.to());
        audit.record("SHEET_CLONED sheet=" + clone.to() + " by=" + request.by() + " from=" + clone.from());
        rollup.recordAll(clone.to(), request.by());
    }
}
