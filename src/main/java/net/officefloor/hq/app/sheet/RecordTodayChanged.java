package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Changing the sheet's today writes no line of its own, only the figures it moved (see Rollup). */
public class RecordTodayChanged {
    public void record(@Val Sheet sheet, @Val TodayBy by, @Val RollupBefore before, Rollup rollup) {
        rollup.record(sheet.id(), by.by(), before);
    }
}
