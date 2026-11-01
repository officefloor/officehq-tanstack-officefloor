package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the rate is withdrawn, so a refused withdrawal writes no audit line. */
public class RecordRateWithdrawn {
    public void record(@Val RateWithdrawRequest request, @Val RateWithdrawal withdrawal, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + withdrawal.sheetId() + " by=" + request.user()
                + " grade=" + withdrawal.grade()
                + (withdrawal.from() != null ? " from=" + withdrawal.from() : ""));
    }
}
