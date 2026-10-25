package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Val;

/** After the change's own audit lines: a ROLLUP line per person whose figures moved, then TOTALS when the totals did. */
public class RecordRollup {
    public void record(@Val SheetResponse sheet, @Val RollupBefore before, @Val ChangedBy by, Rollup rollup) {
        rollup.record(sheet.id(), before.snapshot(), by);
    }
}
