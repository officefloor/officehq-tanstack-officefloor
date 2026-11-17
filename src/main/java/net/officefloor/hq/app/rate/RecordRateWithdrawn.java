package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes RATE_WITHDRAWN to the audit file once the rate is withdrawn, ending with from=<date> when it has a from-date.
 *  Then writes the figures the change moved (see Rollup). */
public class RecordRateWithdrawn {
    public void record(@Val RateWithdrawn withdrawn, @Val RateWithdrawValues values, @Val RollupBefore before,
            Rollup rollup, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + withdrawn.sheetId() + " by=" + values.by() + " grade=" + withdrawn.grade()
                + (withdrawn.from() == null ? "" : " from=" + withdrawn.from()));
        rollup.record(withdrawn.sheetId(), values.by(), before);
    }
}
