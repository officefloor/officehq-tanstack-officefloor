package net.officefloor.hq.app.period;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

/** Loads a period of the sheet by id; one not on the sheet is refused. */
public class LoadPeriod {
    public void load(@PathVariable("sheetId") String sheetId, @PathVariable("periodId") String periodId,
            PeriodRepository periods, Out<Period> period) throws InvalidPeriodException {
        Period found;
        try {
            found = periods.find(sheetId.trim(), Long.parseLong(periodId.trim()));
        } catch (NumberFormatException e) {
            found = null;
        }
        if (found == null) {
            throw new InvalidPeriodException("No period " + periodId);
        }
        period.set(found);
    }
}
