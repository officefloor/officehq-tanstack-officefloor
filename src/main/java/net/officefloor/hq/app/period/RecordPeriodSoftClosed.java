package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodSoftClosed {
    public void record(@Val SoftClosedPeriod softClosed, @Val ChangedBy by, Audit audit) {
        Period period = softClosed.period();
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name()
                + " total=" + period.closedTotal().toPlainString());
    }
}
