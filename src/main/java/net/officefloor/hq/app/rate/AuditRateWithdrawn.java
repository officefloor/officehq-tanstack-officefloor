package net.officefloor.hq.app.rate;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the rate withdrawal, once it is made. */
public class AuditRateWithdrawn {
    public void audit(@Val RateWithdrawal withdrawal, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("RATE_WITHDRAWN sheet=" + sheet.getId() + " by=" + AuditUser.decode(user)
                + " grade=" + withdrawal.grade()
                + (withdrawal.from() == null ? "" : " from=" + withdrawal.from()));
    }
}
