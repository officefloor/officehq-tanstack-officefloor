package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a withdrawn rate: RATE_WITHDRAWN sheet=<id> by=<user> grade=<grade>, then from=<date> when it
 *  has one. */
public class AuditRateWithdrawn {
    public void audit(@PathVariable("sheetId") String sheetId, @Val RateWithdrawInput rate, @Val Rollup before,
            Audit audit, RollupService rollups) {
        audit.record("RATE_WITHDRAWN sheet=" + sheetId.trim() + " by=" + rate.user() + " grade=" + rate.grade()
                + (rate.from() == null ? "" : " from=" + rate.from()));
        rollups.record(sheetId.trim(), rate.user(), before);
    }
}
