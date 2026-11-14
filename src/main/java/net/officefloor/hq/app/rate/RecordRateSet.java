package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a change that went through is recorded. */
public class RecordRateSet {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Rate rate, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + (by == null ? "" : by)
                + " grade=" + rate.grade() + " rate=" + rate.pct() + (rate.from() == null ? "" : " from=" + rate.from()));
    }
}
