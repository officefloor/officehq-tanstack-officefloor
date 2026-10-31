package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Runs before the change: keeps the sheet's figures as they were. */
public class SnapshotRollup {
    public void snapshot(@PathVariable("sheetId") String sheetId, Rollup rollup, Out<RollupBefore> before) {
        before.set(new RollupBefore(sheetId, rollup.snapshot(sheetId)));
    }
}
