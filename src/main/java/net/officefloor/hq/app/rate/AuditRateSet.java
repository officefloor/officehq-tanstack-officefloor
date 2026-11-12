package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored rate: RATE_SET sheet=<id> by=<user> grade=<grade> rate=<rate>, then
 *  from=<date> when it was given one, then to=<date> when it was given one. */
public class AuditRateSet {
    public void audit(@PathVariable("sheetId") String sheetId, @Val RateInput rate, @Val Rollup before, Audit audit,
            RollupService rollups) {
        audit.record(line(sheetId.trim(), rate));
        rollups.record(sheetId.trim(), rate.user(), before);
    }

    /** The RATE_SET record of the rate stored on the sheet. */
    static String line(String sheetId, RateInput rate) {
        return "RATE_SET sheet=" + sheetId + " by=" + rate.user() + " grade=" + rate.grade() + " rate="
                + rate.pct().toPlainString() + (rate.from() == null ? "" : " from=" + rate.from())
                + (rate.to() == null ? "" : " to=" + rate.to());
    }
}
