package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the period is saved, so a refused one writes no audit line. */
public class RecordPeriodAdded {
    public void record(@Val PeriodRequest request, @Val Period period, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + request.user()
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
    }
}
