package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;

/** Refuses moving a claim to a date after its person's retirement. */
public class CheckClaimEditRetired {
    public void check(@Val ValidClaim request, @Val Claim claim, SheetClock clock, PersonRepository people)
            throws InvalidClaimException {
        LocalDate date = request.date() != null ? request.date() : clock.today(claim.getSheetId());
        if (date.equals(claim.getDate())) return;
        Retired.refuse(people.findById(claim.getSheetId(), claim.getPersonId()), date);
    }
}
