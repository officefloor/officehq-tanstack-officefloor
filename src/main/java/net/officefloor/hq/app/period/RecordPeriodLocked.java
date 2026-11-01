package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the period is locked, so a refused lock writes no audit line. */
public class RecordPeriodLocked {
    public void record(@Val PeriodCloseRequest request, @Val LockedPeriod locked, Audit audit) {
        Period period = locked.period();
        audit.record("PERIOD_LOCKED sheet=" + period.sheetId() + " by=" + request.user()
                + " period=" + period.name());
    }
}
