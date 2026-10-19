package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the rate once it has passed every rule and is stored. */
public class AuditRateSet {
    public void audit(@Val Rate rate, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + user
                + " grade=" + rate.grade() + " rate=" + rate.pct().toPlainString());
    }
}
