package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the rate is saved, so a refused change writes no audit line. */
public class RecordRateSet {
    public void record(@Val RateRequest request, @Val Rate rate, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + request.user()
                + " grade=" + rate.grade() + " rate=" + rate.pct().toPlainString());
    }
}
