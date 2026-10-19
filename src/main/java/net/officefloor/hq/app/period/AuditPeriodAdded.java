package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the added period once every rule has passed and it is stored. */
public class AuditPeriodAdded {
    public void audit(@Val Period period, @RequestParam(value = "user", required = false) String user, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + user
                + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
    }
}
