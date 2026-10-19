package net.officefloor.hq.app.period;

import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total as the screen shows it now. A closed period cannot be closed again. */
public class ClosePeriod {
    public void close(@Val Period period, Rollup rollup, PeriodRepository repository, Out<ClosedPeriod> closedOut)
            throws InvalidPeriodException {
        if (period.closed()) {
            throw new InvalidPeriodException("The period " + period.name() + " is already closed.");
        }
        Period closed = period.close(rollup.dueTotal(period.sheetId(), period.from(), period.to()));
        repository.saveClosed(closed);
        closedOut.set(new ClosedPeriod(closed));
    }
}
