package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes the sheet's figures before a change, so its audit step can record how the change moved them. */
public class SnapshotRollup {
    public void snapshot(@PathVariable("sheetId") String sheetId, RollupService rollups, Out<Rollup> before) {
        before.set(rollups.snapshot(sheetId.trim()));
    }
}
