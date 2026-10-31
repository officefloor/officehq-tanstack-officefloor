package net.officefloor.hq.app.rollup;

import net.officefloor.plugin.variable.Val;

/** Runs after the change's own audit lines: audits the figures it moved. */
public class RecordRollup {
    public void record(@Val RollupBefore before, @Val Actor actor, Rollup rollup) {
        rollup.record(before.sheetId(), before.snapshot(), actor.user());
    }
}
