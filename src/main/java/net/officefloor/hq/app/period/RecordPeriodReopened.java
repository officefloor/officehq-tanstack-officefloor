package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodReopened {
    public void record(@Val ReopenedPeriod reopened, @Val ChangedBy by, Audit audit) {
        Period period = reopened.period();
        audit.record("PERIOD_REOPENED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name());
    }
}
