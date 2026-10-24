package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the period's soft close, with the due-total it fixed. */
public class AuditPeriodSoftClosed {
    public void audit(@Val Period period, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERIOD_SOFT_CLOSED sheet=" + period.getSheetId() + " by=" + AuditUser.decode(user)
                + " period=" + period.getName() + " total=" + period.getClosedTotal().toPlainString());
    }
}
