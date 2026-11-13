package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;

/** Refuses a period whose name another period on the sheet already has (ignoring case), or whose dates overlap another's. */
public class CheckPeriodFits {
    public void check(@Val Period period, PeriodRepository repository) throws InvalidPeriodException {
        for (Period other : repository.findAll(period.sheetId())) {
            if (other.name().equalsIgnoreCase(period.name())) {
                throw new InvalidPeriodException("The name " + period.name() + " is already in use.");
            }
            if (other.overlaps(period)) {
                throw new InvalidPeriodException("The dates overlap the period " + other.name() + ".");
            }
        }
    }
}
