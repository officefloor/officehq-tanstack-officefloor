package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordPeriodLocked {
    public void record(@Val LockedPeriod locked, @Val ChangedBy by, Audit audit) {
        Period period = locked.period();
        audit.record("PERIOD_LOCKED sheet=" + period.sheetId() + " by=" + by.name() + " period=" + period.name());
    }
}
