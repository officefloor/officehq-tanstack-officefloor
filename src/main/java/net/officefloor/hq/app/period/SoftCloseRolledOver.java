package net.officefloor.hq.app.period;

import java.util.Comparator;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Runs after the ROLLOVER line: every period of the old fiscal year still open (neither closed nor
 *  soft-closed) is soft-closed, in order of their from-dates, exactly as a soft close would, each
 *  writing its PERIOD_SOFT_CLOSED line. */
public class SoftCloseRolledOver {
    public void close(@PathVariable("sheetId") String sheetId, @Val RolloverYear old, Periods periods,
            PeriodCloser closer, Audit audit) {
        for (Period stale : old.periods().stream()
                .sorted(Comparator.comparing(Period::from).thenComparing(Period::name)).toList()) {
            Period open = periods.find(sheetId, stale.name());
            if (open == null || open.closed()) continue;
            Period period = closer.close(open, true);
            audit.record("PERIOD_SOFT_CLOSED sheet=" + sheetId + " by=" + old.user() + " period=" + period.name()
                    + " total=" + period.closedTotal().setScale(2).toPlainString());
        }
    }
}
