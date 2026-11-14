package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the period just added; runs only once it has passed every rule. */
public class AuditPeriodAdded {
    public void audit(@Val Period period, @RequestParam("user") String user, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + user + " period=" + period.name()
                + " from=" + period.from() + " to=" + period.to() + (period.cap() == null ? "" : " cap=" + period.cap()));
    }
}
