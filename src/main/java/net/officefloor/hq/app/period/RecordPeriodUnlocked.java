package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes PERIOD_UNLOCKED to the audit file once the period is unlocked. */
public class RecordPeriodUnlocked {
    public void record(@Val Period period, @Val PeriodCloseRequest request, Audit audit) {
        audit.record("PERIOD_UNLOCKED sheet=" + period.sheetId() + " by=" + request.by() + " period=" + period.name());
    }
}
