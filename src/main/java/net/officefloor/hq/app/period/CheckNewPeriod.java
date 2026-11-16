package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** A period being added may not take a name already on the sheet, nor share a day with another of its periods. */
public class CheckNewPeriod {
    public void check(@PathVariable("sheetId") String sheetId, @Val PeriodValues values, PeriodRepository periods)
            throws InvalidPeriodException {
        String sheet = sheetId.trim();
        if (periods.nameTaken(sheet, values.name())) {
            throw new InvalidPeriodException("A period named " + values.name() + " is already on the sheet");
        }
        if (periods.overlaps(sheet, values.from(), values.to())) {
            throw new InvalidPeriodException("The period overlaps another period of the sheet");
        }
    }
}
