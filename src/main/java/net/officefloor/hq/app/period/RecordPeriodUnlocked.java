package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the period is unlocked, so a refused unlock writes no audit line. */
public class RecordPeriodUnlocked {
    public void record(@Val PeriodCloseRequest request, @Val UnlockedPeriod unlocked, Audit audit) {
        Period period = unlocked.period();
        audit.record("PERIOD_UNLOCKED sheet=" + period.sheetId() + " by=" + request.user()
                + " period=" + period.name());
    }
}
