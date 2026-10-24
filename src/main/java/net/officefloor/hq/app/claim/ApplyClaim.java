package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Saves the claim's changes; refuses them unless the claim is a DRAFT (an open claim is voided and added anew
 * instead), when its due-by (fixed when it was created; editing does not move it) is in a closed period, and when it would move after its
 * person's retirement date, a correction that does not name a settled claim of its person, and a reference another claim on the sheet has. Saving records who
 * last edited it, and closes the claim's editor: it releases the claim's lock. */
public class ApplyClaim {
    public void apply(@Val ClaimInput input, @Val Sheet sheet, @Val Claim claim, ClaimRepository repository,
            PeriodRepository periods, PersonRepository people,
            @RequestHeader(name = "X-User", required = false) String userHeader) throws InvalidClaimException {
        if (claim.getStatus() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("Only a draft claim can be edited; void it and add a new one.");
        }
        LocalDate date = input.date() != null ? input.date() : sheet.getToday();
        if (periods.isClosedOn(sheet.getId(), claim.getDueBy())) {
            throw new InvalidClaimException("No claim can be edited in a closed period.");
        }
        if (people.findById(claim.getPersonId()).map(p -> p.retiredOn(date)).orElse(false)) {
            throw new InvalidClaimException("No claim can be dated after the person's retirement date.");
        }
        LocalDate correctsDate = repository.checkCorrects(sheet.getId(), claim.getPersonId(), input.corrects());
        repository.checkRef(sheet.getId(), claim.getId(), input.ref());
        claim.setAmount(input.amount());
        claim.setCorrects(input.corrects());
        claim.setCorrectsDate(correctsDate);
        claim.setDate(date);
        claim.setNote(input.note());
        claim.setRef(input.ref());
        claim.setTags(input.tags());
        claim.setLockedBy(null);
        claim.setEditedBy(AuditUser.decode(userHeader));
        repository.saveAndFlush(claim);
    }
}
