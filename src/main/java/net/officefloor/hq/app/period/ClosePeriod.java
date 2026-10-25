package net.officefloor.hq.app.period;

import java.math.BigDecimal;
import java.util.Map;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Closes the period, fixing its due-total and each person's as they are now; refuses a period already closed. */
public class ClosePeriod {
    public void close(@Val Period period, PeriodRepository repository, Rollup rollup, Out<ClosedPeriod> closed)
            throws BadRequestException {
        if (period.closed()) {
            throw new BadRequestException("That period is already closed");
        }
        Map<Long, BigDecimal> personTotals = rollup.dueBetweenByPerson(period.sheetId(), period.from(), period.to());
        BigDecimal total = personTotals.values().stream().reduce(BigDecimal.ZERO.setScale(2), BigDecimal::add);
        if (!repository.close(period.id(), total, personTotals)) {
            throw new BadRequestException("The period was closed by someone else; reload the sheet");
        }
        closed.set(new ClosedPeriod(new Period(period.id(), period.sheetId(), period.name(), period.from(),
                period.to(), true, total, personTotals)));
    }
}
