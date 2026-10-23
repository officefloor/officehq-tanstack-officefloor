package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored locked lock change in the audit file. */
public class AuditPeriodLocked {
    public void audit(@Val PeriodCloseInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_LOCKED sheet=" + period.sheetId() + " by=" + input.user() + " period=" + period.name());
    }
}
