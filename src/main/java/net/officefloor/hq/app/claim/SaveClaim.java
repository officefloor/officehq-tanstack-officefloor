package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveClaim {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            @Val ClaimInput input, Claims claims, People people, Periods periods, Audit audit, Rollup rollup)
            throws InvalidClaimException {
        // A claim dated inside a closed period may not be edited, nor moved into one.
        if (periods.closedOn(sheet, LocalDate.parse(claim.date())) || periods.closedOn(sheet, input.date())) {
            throw new InvalidClaimException("A claim dated inside a closed period may not be edited");
        }
        // Only a draft may be edited: an open claim is voided and a new one added instead.
        if (claim.status() != ClaimStatus.DRAFT) {
            throw new InvalidClaimException("A " + claim.status() + " claim cannot be edited; only a DRAFT can");
        }
        Person owner = people.find(sheet, claim.person());
        if (owner.retiredBefore(input.date())) {
            throw new InvalidClaimException("A claim may not be dated after the person's retirement date");
        }
        input.checkCorrects(sheet, owner.id(), claims);
        Rollup.Snapshot before = rollup.take(sheet);
        if (claim.lockedBy() != null && !claim.lockedBy().equals(user)) {
            throw new InvalidClaimException("The claim is being edited by " + claim.lockedBy());
        }
        // Conditional on the claim still being a draft and not locked by another user's editor, so one another user
        // opened or locked meanwhile is refused; the save releases the lock.
        if (!claims.update(sheet, claim.id(), input, user)) {
            throw new InvalidClaimException("The claim is no longer a DRAFT, or another user is editing it");
        }
        audit.record("CLAIM_EDITED sheet=" + sheet + " by=" + user + " " + input.auditFields(owner.name()));
        rollup.record(sheet, user, before);
    }
}
