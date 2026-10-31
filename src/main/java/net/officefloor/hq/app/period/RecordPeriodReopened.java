package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the period is reopened, so a refused reopen writes no audit line. */
public class RecordPeriodReopened {
    public void record(@Val PeriodCloseRequest request, @Val ReopenedPeriod reopened, Audit audit) {
        Period period = reopened.period();
        audit.record("PERIOD_REOPENED sheet=" + period.sheetId() + " by=" + request.user()
                + " period=" + period.name());
    }
}
