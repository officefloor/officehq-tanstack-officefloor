package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the period's being unlocked. */
public class AuditPeriodUnlocked {
    public void audit(@Val Period period, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("PERIOD_UNLOCKED sheet=" + period.getSheetId() + " by=" + AuditUser.decode(user)
                + " period=" + period.getName());
    }
}
