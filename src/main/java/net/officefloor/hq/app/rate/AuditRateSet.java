package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the rate just set; runs only once it has passed every rule. */
public class AuditRateSet {
    public void audit(@Val Rate rate, @RequestParam("user") String user, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + user + " grade=" + rate.grade()
                + " rate=" + rate.rate().setScale(2).toPlainString()
                + (rate.from() == null ? "" : " from=" + rate.from()));
    }
}
