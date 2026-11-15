package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Edits a claim: only a DRAFT can be edited (to change an open claim, void it and add a new one),
 *  and not to a date after its person's retirement date; a correction as when added. Its due-by (and so
 *  its period) stays as fixed when it was added; refused when that falls in a closed period. Refused while
 *  another user's open editor locks it. */
public class ApplyClaim {
    public void apply(@RequestParam(name = "by", required = false) String by, @Val ValidClaim request,
            @Val Claim claim, SheetRepository sheets,
            PeriodRepository periods, PersonRepository people, ClaimRepository claims)
            throws ClaimStatusException, PeriodClosedException, InvalidClaimException, ClaimLockedException {
        if (!ClaimStatus.DRAFT.name().equals(claim.getStatus())) {
            throw new ClaimStatusException("A " + claim.getStatus() + " claim cannot be edited");
        }
        periods.requireOpen(claim.getSheetId(), claim.getDueBy());
        if (claim.getLockedBy() != null && !claim.getLockedBy().equals(by == null ? "" : by)) {
            throw new ClaimLockedException("The claim is being edited by " + claim.getLockedBy());
        }
        LocalDate date = request.date() != null ? request.date() : sheets.today(claim.getSheetId());
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        if (person != null && person.retiredBefore(date)) {
            throw new InvalidClaimException("The person is retired from " + person.getRetiredFrom());
        }
        String corrects = Corrections.check(request, claim.getSheetId(), claim.getPersonId(), claims);
        claim.setAmount(request.amount().toPlainString());
        claim.setCorrects(corrects);
        claim.setRoundDate(corrects == null ? null : claims.findByNumber(claim.getSheetId(), corrects).getDate());
        claim.setDate(date.toString());
        claim.setNote(request.note());
    }
}
