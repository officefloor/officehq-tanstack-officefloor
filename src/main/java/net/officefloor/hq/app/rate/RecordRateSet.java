package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Writes RATE_SET to the audit file once the rate is stored. */
public class RecordRateSet {
    public void record(@Val Rate rate, @Val RateValues values, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + values.by() + " grade=" + rate.grade()
                + " rate=" + rate.rate().toPlainString());
    }
}
