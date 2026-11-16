package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes RATE_SET to the audit file once the rate is stored, ending with from=<date> when it has a from-date. Then writes the
 * figures the change moved (see Rollup). */
public class RecordRateSet {
    public void record(@Val Rate rate, @Val RateValues values, @Val RollupBefore before, Rollup rollup, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.sheetId() + " by=" + values.by() + " grade=" + rate.grade()
                + " rate=" + rate.rate().toPlainString() + (rate.from() == null ? "" : " from=" + rate.from()));
        rollup.record(rate.sheetId(), values.by(), before);
    }
}
