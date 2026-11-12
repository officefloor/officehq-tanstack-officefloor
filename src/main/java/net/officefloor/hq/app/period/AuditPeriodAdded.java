package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a stored period: PERIOD_ADDED sheet=<id> by=<user> period=<name> from=<date> to=<date>. */
public class AuditPeriodAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val PeriodInput period, Audit audit) {
        audit.record("PERIOD_ADDED sheet=" + sheetId.trim() + " by=" + period.user() + " period=" + period.name()
                + " from=" + period.from() + " to=" + period.to());
    }
}
