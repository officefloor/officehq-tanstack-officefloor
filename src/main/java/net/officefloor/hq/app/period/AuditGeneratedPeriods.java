package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records each generated period as added, earliest first, in the form a period added by hand takes. */
public class AuditGeneratedPeriods {
    public void audit(@Val GeneratedPeriods generated, @RequestParam("user") String user, Audit audit) {
        for (Period period : generated.periods()) {
            audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + user + " period=" + period.name()
                    + " from=" + period.from() + " to=" + period.to());
        }
    }
}
