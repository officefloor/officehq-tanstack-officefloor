package net.officefloor.hq.app.period;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class ClosePeriod {
    // Fixes each person's due-total for the period as it stands now, and the period's as their sum; stored only
    // if the period is still open.
    public void close(@PathVariable("sheetId") String sheetId, @PathVariable("name") String name, Periods periods,
            Rollup rollup, Out<Period> periodOut) throws PeriodRefusedException {
        Period period = periods.find(sheetId.trim(), name.trim());
        if (period == null) {
            throw new PeriodRefusedException("No period " + name + " on this sheet.");
        }
        Period closed = period.closedAt(rollup.personDueTotals(period.sheetId(), period.from(), period.to()));
        if (period.closed() || !periods.close(closed)) {
            throw new PeriodRefusedException("The period " + period.name() + " is already closed.");
        }
        periodOut.set(closed);
    }
}
