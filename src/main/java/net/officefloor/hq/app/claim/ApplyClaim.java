package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Applies the edited values; only a DRAFT claim can be edited (an open claim is voided and a new one added instead),
 * and a claim dated inside a closed period, or moved into one, or left or moved after its person's retirement date, is refused. */
public class ApplyClaim {
    public void apply(@Val Claim claim, @Val ClaimValues values, SheetRepository sheets, PeriodRepository periods,
            PersonRepository people, ClaimRepository claims, Out<Claim> changed) throws InvalidClaimException {
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a DRAFT claim can be edited");
        }
        LocalDate date = values.date() != null ? values.date() : sheets.today(claim.sheetId());
        if (periods.closedOn(claim.sheetId(), claim.date()) || periods.closedOn(claim.sheetId(), date)) {
            throw new InvalidClaimException("The claim's date is inside a closed period");
        }
        Person person = people.find(claim.sheetId(), claim.personId());
        if (person != null && !person.mayClaimOn(date)) {
            throw new InvalidClaimException("The claim's date is after the person's retirement date");
        }
        changed.set(new Claim(claim.id(), claim.sheetId(), claim.personId(), values.amount(), date, values.note(), claim.status(),
                claim.number(), claim.due(), claim.openedBy(), claim.stated(), claim.statedDue(),
                Corrections.check(claim.sheetId(), claim.personId(), values, claims), claim.dueBy(), claim.deferredUntil(), claim.proofs(), claim.splitFrom()));
    }
}
