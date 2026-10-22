package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored close in the audit file. */
public class AuditPeriodClosed {
    public void audit(@Val PeriodCloseInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_CLOSED sheet=" + period.sheetId() + " by=" + input.user()
                + " period=" + period.name() + " total=" + period.closedTotal().toPlainString());
    }
}
