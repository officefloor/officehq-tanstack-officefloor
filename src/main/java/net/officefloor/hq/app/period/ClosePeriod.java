package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total as it is now; refuses a period already closed. */
public class ClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, Rollup rollup, Out<ClosedPeriod> closed)
            throws BadRequestException {
        if (period.closed()) {
            throw new BadRequestException("That period is already closed");
        }
        BigDecimal total = rollup.dueBetween(period.sheetId(), period.from(), period.to());
        if (!repository.close(period.id(), total)) {
            throw new BadRequestException("The period was closed by someone else; reload the sheet");
        }
        closed.set(new ClosedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(),
                period.to(), true, total)));
    }
}
