package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;

/** Edits a claim: only a DRAFT can be edited (to change an open claim, void it and add a new one),
 *  and not to a date after its person's retirement date. */
public class ApplyClaim {
    public void apply(@Val ValidClaim request, @Val Claim claim, SheetRepository sheets,
            PeriodRepository periods, PersonRepository people)
            throws ClaimStatusException, PeriodClosedException, InvalidClaimException {
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be edited");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDate());
        LocalDate date = request.date() != null ? request.date() : sheets.today(claim.getSheetId());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null && person.retiredBefore(date)) {
            throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
        }
        claim.setAmount(request.amount().toPlainString());
        claim.setDate(date.toString());
        claim.setNote(request.note());
        periods.requireOpen(claim.getSheetId(), claim.getDate());
    }
}
