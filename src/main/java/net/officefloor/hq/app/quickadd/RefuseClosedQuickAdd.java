package net.officefloor.hq.app.quickadd;

import net.officefloor.hq.app.period.ClosedPeriodException;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse the quick-add when the claim's date (the sheet's entry date, else its today) is inside a closed period — before any
 *  person is added, so a refused quick-add leaves nothing behind. */
public class RefuseClosedQuickAdd {
    public void refuse(@PathVariable("sheetId") String sheetId, SheetRepository sheets, PeriodRepository periods)
            throws ClosedPeriodException {
        String id = sheetId.trim();
        periods.lockSheet(id);
        Period closed = periods.closedOn(id, sheets.newClaimDate(id));
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
