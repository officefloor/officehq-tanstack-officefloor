package net.officefloor.hq.app.period;

import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a period whose name another period on the sheet has, or whose dates overlap another's. */
public class CheckPeriodFree {
    public void check(@PathVariable("sheetId") String sheetId, @Val PeriodInput period, PeriodStore store)
            throws InvalidSheetException {
        if (store.nameTaken(sheetId.trim(), period.name())) {
            throw new InvalidSheetException("The period name " + period.name() + " is already in use");
        }
        if (store.overlaps(sheetId.trim(), period.from(), period.to())) {
            throw new InvalidSheetException("The period overlaps another period");
        }
    }
}
