package net.officefloor.hq.app.period;

import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Refuse a new claim dated (a blank date being the sheet's entry date, else its today) inside a closed period. */
public class RefuseClosedNewClaim {
    public void refuse(@Val ClaimInput input, @Val Person person, SheetRepository sheets, PeriodRepository periods)
            throws ClosedPeriodException {
        periods.lockSheet(person.sheetId());
        var date = input.date() != null ? input.date() : sheets.newClaimDate(person.sheetId());
        Period closed = periods.closedOn(person.sheetId(), date);
        if (closed != null) {
            throw new ClosedPeriodException(closed);
        }
    }
}
