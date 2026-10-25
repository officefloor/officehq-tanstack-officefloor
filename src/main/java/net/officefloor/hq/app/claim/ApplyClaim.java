package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonGradeRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Saves the claim's changes; refuses them unless the claim is a DRAFT (an open claim is voided and added anew
 * instead), when its due-by (as it stands before the edit) is in a closed period, when its date moves to one whose due-by
 * (its person's terms on that date, counted in business days) would be in a closed period, and when it would move after its
 * person's retirement date, an amount other than its lines' total when it has lines, a correction that does not name a settled claim of its person, and a reference another claim on the sheet has. Saving records who
 * last edited it and closes the claim's editor: it releases the claim's lock. Its due-by counts from its date as saved,
 * with the terms fixed when it was created ({@link Claim#getDueTerms}); a due-by a steward set stays as set. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, ClaimRepository repository,
            PeriodRepository periods, PersonRepository people, PersonGradeRepository grades, HolidayRepository holidays,
            ClaimLineRepository lines,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a draft claim can be edited; void it and add a new one.");
        }
        LocalDate date = input.date() != null ? input.date() : sheet.getToday();
        if (periods.isClosedOn(sheet.getId(), claim.getDueBy())) {
            throw new InvalidClaimException("No claim can be edited in a closed period.");
        }
        BigDecimal lined = lines.totalOf(claim);
        if (lined != null && input.amount().compareTo(lined) != 0) {
            throw new InvalidClaimException("A claim with lines has their total as its amount (" + lined.toPlainString() + ").");
        }
        Person person = people.findById(claim.getPersonId()).orElse(null);
        if (person != null && !date.equals(claim.getDate())) {
            if (periods.isClosedOn(sheet.getId(), holidays.dueBy(sheet.getId(), date, claim.getDueTerms()))) {
                throw new InvalidClaimException("No claim can be moved to a date whose due-by is in a closed period.");
            }
        }
        if (person != null && person.retiredOn(date)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date.");
        }
        LocalDate correctsDate = repository.checkCorrects(sheet.getId(), claim.getPersonId(), input.corrects(),
                input.amount(), claim.getId());
        repository.checkRef(sheet.getId(), claim.getId(), input.ref());
        claim.setAmount(input.amount());
        claim.setCorrects(input.corrects());
        claim.setCorrectsDate(correctsDate);
        claim.setDate(date);
        if (!date.equals(claim.getDueFrom())) {
            claim.fixDue(date, claim.getDueTerms(), holidays.datesOf(sheet.getId()));
        }
        claim.setNote(input.note());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setLockedBy(null);
        claim.setEditedBy(AuditUser.decode(userHeader));
        repository.saveAndFlush(claim);
    }
}
