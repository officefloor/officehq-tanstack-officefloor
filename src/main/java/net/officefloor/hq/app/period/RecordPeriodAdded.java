package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodAdded {
    public void record(@Val Period period, @Val ChangedBy by, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name()
                + " from=" + period.from() + " to=" + period.to());
    }
}
