package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Last write step: only a withdrawal that went through is recorded. */
public class RecordRateWithdrawn {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val RateWithdrawal withdrawal, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + withdrawal.sheetId() + " by=" + (by == null ? "" : by)
                + " grade=" + withdrawal.grade() + (withdrawal.from() == null ? "" : " from=" + withdrawal.from()));
    }
}
