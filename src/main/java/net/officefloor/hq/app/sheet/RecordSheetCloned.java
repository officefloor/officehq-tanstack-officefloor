package net.officefloor.hq.app.sheet;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.plugin.variable.Val;
import java.util.Map;

/** Runs after the clone is saved, so a refused one writes no audit line: this sheet's line, then the
 *  clone's line and its rollup lines for every person and its totals. */
public class RecordSheetCloned {
    public void record(@Val Clone clone, Audit audit, Rollup rollup) {
        audit.record("SHEET_CLONE_MADE sheet=" + clone.from() + " by=" + clone.user() + " to=" + clone.to());
        audit.record("SHEET_CLONED sheet=" + clone.to() + " by=" + clone.user() + " from=" + clone.from());
        // Nothing before: every person and the totals count as moved.
        rollup.record(clone.to(), new Snapshot(Map.of(), null, null, null), clone.user());
    }
}
