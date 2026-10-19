package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Notes the sheet's figures before the change writes anything, for {@link AuditRollup} to compare. */
public class TakeRollup {
    public void take(@PathVariable("sheetId") String sheetId, Rollup rollup, Out<Rollup.Snapshot> before) {
        before.set(rollup.take(sheetId));
    }
}
