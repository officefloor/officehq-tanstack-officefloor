package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the rate version just withdrawn. */
public class AuditRateWithdrawn {
    public void audit(@Val RateWithdrawn withdrawn, @RequestParam("user") String user, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + withdrawn.sheetId() + " by=" + user + " grade=" + withdrawn.grade()
                + (withdrawn.from() == null ? "" : " from=" + withdrawn.from()));
    }
}
