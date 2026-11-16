package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERIOD_SOFT_CLOSED to the audit file once the period is soft-closed, with the due-total it fixed. */
public class RecordPeriodSoftClosed {
    public void record(@Val Period period, @Val PeriodClose closed, Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.sheetId() + " by=" + closed.by() + " period=" + period.name()
                + " total=" + closed.total().toPlainString());
    }
}
