package net.officefloor.hq.app.rollup;

import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Takes the sheet's figures before the change writes anything, so RecordRollup can tell what it moved. */
public class SnapshotRollup {
    public void snapshot(@Val SheetResponse sheet, Rollup rollup, Out<RollupBefore> before) {
        before.set(new RollupBefore(rollup.snapshot(sheet.id())));
    }
}
