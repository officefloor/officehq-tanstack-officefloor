package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERIOD_CLOSED to the audit file once the period is closed, with the due-total it fixed. */
public class RecordPeriodClosed {
    public void record(@Val Period period, @Val PeriodClose closed, Audit audit) {
        audit.record("PERIOD_CLOSED sheet=" + period.sheetId() + " by=" + closed.by() + " period=" + period.name()
                + " total=" + closed.total().toPlainString());
    }
}
