package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored add in the audit file. */
public class AuditPeriodAdded {
    public void audit(@Val PeriodInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + input.user()
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
    }
}
