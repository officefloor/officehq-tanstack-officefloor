package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.HttpHeaderParameter;

/** Records RATE_SET once the rate is saved; the signed-in user arrives as X-User. */
public class AuditRateSet {
    public void audit(@Val Rate rate, @HttpHeaderParameter("X-User") String user, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.getSheetId() + " by=" + (user == null ? "" : user.trim())
                + " grade=" + rate.getGrade() + " rate=" + rate.getRate().setScale(2).toPlainString()
                + (rate.getFromDate() == null ? "" : " from=" + rate.getFromDate()));
    }
}
