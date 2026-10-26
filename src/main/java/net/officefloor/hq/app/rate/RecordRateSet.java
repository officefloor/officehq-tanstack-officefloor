package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordRateSet {
    public void record(@Val Rate rate, @Val ChangedBy by, Audit audit) {
        audit.record(line(rate, by));
    }

    /** The RATE_SET audit line for a rate set by the user. */
    static String line(Rate rate, ChangedBy by) {
        return "RATE_SET sheet=" + rate.sheetId() + " by=" + by.name() + " grade=" + rate.grade() + " rate="
                + rate.rate().toPlainString() + (rate.from() == null ? "" : " from=" + rate.from())
                + (rate.to() == null ? "" : " to=" + rate.to());
    }
}
