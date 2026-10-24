package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records each period a generate made, in month order, as an added period. */
public class AuditPeriodsGenerated {
    public void audit(@Val GeneratedPeriods generated, @RequestHeader(name = "X-User", required = false) String user,
            Audit audit) {
        for (Period period : generated.periods()) {
            audit.record("PERIOD_ADDED sheet=" + period.getSheetId() + " by=" + AuditUser.decode(user)
                    + " period=" + period.getName() + " from=" + period.getFromDate() + " to=" + period.getToDate());
        }
    }
}
