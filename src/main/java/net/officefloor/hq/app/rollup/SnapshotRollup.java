package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes the sheet's figures before a change, so the change can audit what it moved. */
public class SnapshotRollup {
    public void snapshot(@PathVariable("sheetId") String sheetId, Rollup rollup, Out<RollupFigures> before) {
        before.set(rollup.figures(sheetId));
    }
}
