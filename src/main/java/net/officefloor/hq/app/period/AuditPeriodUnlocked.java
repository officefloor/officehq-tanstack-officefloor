package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a unlocked period: PERIOD_UNLOCKED sheet=<id> by=<user> period=<name>. */
public class AuditPeriodUnlocked {
    public void audit(@PathVariable("sheetId") String sheetId, @Val CloseRequest request, @Val PeriodResponse period,
            Audit audit) {
        audit.record("PERIOD_UNLOCKED sheet=" + sheetId.trim() + " by=" + request.user() + " period=" + period.name());
    }
}
