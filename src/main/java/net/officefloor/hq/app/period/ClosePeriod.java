package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ClosePeriod {
    // Fixes the period's due-total as it stands now; stored only if the period is still open.
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Rollup rollup, Out<Period> periodOut) throws PeriodRefusedException {
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        BigDecimal total = rollup.dueTotal(period.sheetId(), period.from(), period.to());
        if (period.closed() || !periods.close(period, total)) {
            throw new PeriodRefusedException("The period " + period.name() + " is already closed.");
        }
        periodOut.set(period.closedAt(total));
    }
}
