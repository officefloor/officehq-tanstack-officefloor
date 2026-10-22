package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record the stored rate in the audit file. */
public class AuditRateSet {
    public void audit(@Val RateInput input, @Val Rate rate, Audit audit) {
        RateRules.record(input, rate, audit);
    }
}
