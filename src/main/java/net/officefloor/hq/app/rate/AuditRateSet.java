package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored rate: RATE_SET sheet=<id> by=<user> grade=<grade> rate=<rate>. */
public class AuditRateSet {
    public void audit(@PathVariable("sheetId") String sheetId, @Val RateInput rate, Audit audit) {
        audit.record("RATE_SET sheet=" + sheetId.trim() + " by=" + rate.user() + " grade=" + rate.grade() + " rate="
                + rate.pct().toPlainString());
    }
}
