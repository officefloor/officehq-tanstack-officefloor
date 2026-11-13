package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes the sheet's figures before the change, so its rollup can tell which of them it moved. */
public class SnapshotRollup {
    public void snapshot(@PathVariable("sheetId") String sheetId, RollupService rollups, Out<RollupBefore> before) {
        before.set(new RollupBefore(rollups.compute(sheetId)));
    }
}
