package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodUnlocked {
    public void record(@Val LockedPeriod unlocked, @Val ChangedBy by, Audit audit) {
        Period period = unlocked.period();
        audit.record("PERIOD_UNLOCKED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name());
    }
}
