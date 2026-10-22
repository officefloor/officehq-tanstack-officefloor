package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored reopen in the audit file. */
public class AuditPeriodReopened {
    public void audit(@Val PeriodCloseInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_REOPENED sheet=" + period.sheetId() + " by=" + input.user() + " period=" + period.name());
    }
}
