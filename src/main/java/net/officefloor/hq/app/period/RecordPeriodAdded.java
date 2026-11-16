package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERIOD_ADDED to the audit file once the period is stored. */
public class RecordPeriodAdded {
    public void record(@Val Period period, @Val PeriodValues values, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + values.by() + " period=" + period.name()
                + " from=" + period.from() + " to=" + period.to());
    }
}
