package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a change that went through is recorded. */
public class RecordPeriodAdded {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Period period, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + (by == null ? "" : by)
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to()
                + (period.cap() == null ? "" : " cap=" + period.cap()));
    }
}
