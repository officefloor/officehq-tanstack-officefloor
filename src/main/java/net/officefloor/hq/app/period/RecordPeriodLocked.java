package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a lock that went through is recorded. */
public class RecordPeriodLocked {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Period period, Audit audit) {
        audit.record("PERIOD_LOCKED sheet=" + period.sheetId() + " by=" + (by == null ? "" : by)
                + " period=" + period.name());
    }
}
