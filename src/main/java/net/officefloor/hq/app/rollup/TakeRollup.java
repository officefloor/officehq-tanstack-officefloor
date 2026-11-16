package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Takes the sheet's figures before the change, so its audit step can write the ones it moved. */
public class TakeRollup {
    public void take(@PathVariable("sheetId") String sheetId, Rollup rollup, Out<RollupBefore> before) {
        before.set(rollup.take(sheetId.trim()));
    }
}
