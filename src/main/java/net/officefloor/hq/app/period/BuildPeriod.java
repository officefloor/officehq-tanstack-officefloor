package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Builds the period, a blank from-date taking the sheet's today, and checks it against the sheet:
 * its to-date not before its from-date, its name not already used, and no day shared with another
 * period.
 */
public class BuildPeriod {
    public void build(@Val PeriodInput input, @Val Sheet sheet, PeriodRepository repository,
            Out<Period> periodOut) throws InvalidPeriodException {
        Period period = new Period(sheet.id(), input.name(),
                input.from() == null ? sheet.today() : input.from(), input.to());
        if (period.to().isBefore(period.from())) {
            throw new InvalidPeriodException("A period's to-date is on or after its from-date");
        }
        for (Period other : repository.findAll(sheet.id())) {
            if (other.name().equals(period.name())) {
                throw new InvalidPeriodException("There is already a period named " + period.name());
            }
            if (other.overlaps(period)) {
                throw new InvalidPeriodException("A period may not overlap period " + other.name());
            }
        }
        periodOut.set(period);
    }
}
