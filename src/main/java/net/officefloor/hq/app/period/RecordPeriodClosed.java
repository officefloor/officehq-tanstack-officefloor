package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodClosed {
    public void record(@Val ClosedPeriod closed, @Val ChangedBy by, Audit audit) {
        audit.record(line(closed.period(), by));
    }

    /** The PERIOD_CLOSED line for the closed period. */
    static String line(Period period, ChangedBy by) {
        return "PERIOD_CLOSED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name()
                + " total=" + period.closedTotal().toPlainString();
    }
}
