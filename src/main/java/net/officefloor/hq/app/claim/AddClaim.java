package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the claim to the person; a claim given no date is dated the sheet's today. A claim dated inside a closed
 *  period is refused. */
public class AddClaim {
    public void add(@Val Person person, @Val ClaimValues values, SheetRepository sheets, ClaimRepository claims,
            PeriodRepository periods, Out<Claim> claim) throws InvalidClaimException {
        LocalDate date = values.date() != null ? values.date() : sheets.today(person.sheetId());
        if (periods.closedOn(person.sheetId(), date)) {
            throw new InvalidClaimException("The claim's date is inside a closed period");
        }
        claim.set(claims.add(person.sheetId(), person.id(), values.amount(), date, values.note()));
    }
}
