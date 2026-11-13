package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses (400) a claim added or edited to a date (blank: the sheet's today) inside a closed period. */
public class CheckClaimInputPeriodOpen {
    public void check(@Val ClaimInput input, @PathVariable("sheetId") String sheetId, SheetRepository sheets,
            PeriodRepository periods) throws InvalidClaimException {
        LocalDate date = input.date() != null ? input.date() : sheets.today(sheetId);
        Period closed = periods.closedOn(sheetId, date);
        if (closed != null) {
            throw new InvalidClaimException("The date " + date + " is in the closed period " + closed.name() + ".");
        }
    }
}
