package net.officefloor.hq.app.period;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Record each generated period in the audit file as an add, in month order. */
public class AuditPeriodsGenerated {
    public void audit(@Val PeriodGenerateInput input, @Val GeneratedPeriods generated, Audit audit) {
        for (Period period : generated.periods()) {
            audit.record("PERIOD_ADDED sheet=" + period.sheetId() + " by=" + input.user()
                    + " period=" + period.name() + " from=" + period.from() + " to=" + period.to());
        }
    }
}
