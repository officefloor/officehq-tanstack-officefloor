package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses the quick-add when the sheet's today is inside a closed period, before a new person is
 *  saved, so a refusal writes nothing. */
public class CheckQuickClaimPeriod {
    public void check(@PathVariable("sheetId") String sheetId, SheetRepository sheets, PeriodRepository periods)
            throws ClosedPeriodException {
        String sheet = sheetId.trim();
        var today = sheets.findById(sheet).orElseThrow().getToday();
        var closed = periods.findBySheetId(sheet).stream()
                .filter((p) -> p.isClosed() && p.contains(today)).findFirst();
        if (closed.isPresent()) {
            throw new ClosedPeriodException("The claim is dated inside the closed period " + closed.get().getName() + ".");
        }
    }
}
