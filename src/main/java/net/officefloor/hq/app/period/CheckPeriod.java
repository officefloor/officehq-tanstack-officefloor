package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

/** Refuses a period whose name is already used on the sheet (ignoring case), or whose dates overlap
 *  another period of the sheet. */
public class CheckPeriod {
    public void check(@Val Period period, PeriodRepository repository) throws InvalidPeriodException {
        for (Period other : repository.findBySheet(period.sheetId())) {
            if (other.name().equalsIgnoreCase(period.name())) {
                throw new InvalidPeriodException("That name is already in use on this sheet.");
            }
            if (other.overlaps(period)) {
                throw new InvalidPeriodException("The dates overlap the period " + other.name() + ".");
            }
        }
    }
}
