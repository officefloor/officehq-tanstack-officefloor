package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored unlock in the audit file. */
public class AuditPeriodUnlocked {
    public void audit(@Val PeriodCloseInput input, @Val Period period, Audit audit) {
        audit.record("PERIOD_UNLOCKED sheet=" + period.sheetId() + " by=" + input.user() + " period=" + period.name());
    }
}
