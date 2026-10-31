package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the period is soft-closed, so a refused soft close writes no audit line. */
public class RecordPeriodSoftClosed {
    public void record(@Val PeriodCloseRequest request, @Val ClosedPeriod closed, Audit audit) {
        Period period = closed.period();
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.sheetId() + " by=" + request.user()
                + " period=" + period.name() + " total=" + period.closedTotal().setScale(2).toPlainString());
    }
}
