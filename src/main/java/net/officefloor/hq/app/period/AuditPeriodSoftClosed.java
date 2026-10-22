package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored soft close in the audit file. */
public class AuditPeriodSoftClosed {
    public void audit(@Val PeriodCloseInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.sheetId() + " by=" + input.user()
                + " period=" + period.name() + " total=" + period.closedTotal().toPlainString());
    }
}
