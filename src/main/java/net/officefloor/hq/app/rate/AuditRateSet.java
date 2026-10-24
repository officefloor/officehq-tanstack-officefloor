package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the grade's rate, once it is saved. */
public class AuditRateSet {
    public void audit(@Val Rate rate, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("RATE_SET sheet=" + rate.getSheetId() + " by=" + AuditUser.decode(user)
                + " grade=" + rate.getGrade() + " rate=" + rate.getPct().setScale(2).toPlainString()
                + (rate.getFromDate() == null ? "" : " from=" + rate.getFromDate()));
    }
}
