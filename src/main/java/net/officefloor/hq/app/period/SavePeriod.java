package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Val;

/** Adds the period unless the sheet already has one of its name or one sharing a day with it. */
public class SavePeriod {
    public void save(@Val Period period, Periods periods) throws InvalidSheetRequestException {
        for (Period other : periods.onSheet(period.sheetId())) {
            if (other.name().equals(period.name())) {
                throw new InvalidSheetRequestException("There is already a period named " + period.name());
            }
            if (other.overlaps(period)) {
                throw new InvalidSheetRequestException("The period overlaps " + other.name());
            }
        }
        periods.add(period);
    }
}
