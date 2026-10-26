package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class SnapshotRollup {
    // The sheet's figures before the change, for AuditRollup to compare with after it.
    public void snapshot(@PathVariable("sheetId") String sheetId, Rollup rollup, Out<Figures> before) {
        before.set(rollup.figures(sheetId.trim()));
    }
}
