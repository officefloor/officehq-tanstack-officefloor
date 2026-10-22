package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Take the sheet's figures before the change, so the change's audit can record how they moved. */
public class TakeRollup {
    public void take(@PathVariable("sheetId") String sheetId, Rollups rollups, Out<Rollup> before) {
        before.set(rollups.take(sheetId.trim()));
    }
}
