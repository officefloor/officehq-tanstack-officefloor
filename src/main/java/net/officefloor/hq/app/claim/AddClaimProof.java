package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Raises the claim's proofs count by one: not on a SETTLED or VOID claim, nor on one due inside a closed period
 * (which may not be edited).
 */
public class AddClaimProof {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, Periods periods, Audit audit) throws InvalidClaimStatusChangeException {
        if (claim.status() == ClaimStatus.SETTLED || claim.status() == ClaimStatus.VOID) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be given a proof");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot be given a proof");
        }
        // Conditional on the status and count read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.addProof(sheet, claim.id(), claim.status(), claim.proofs())) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        Person person = people.find(sheet, claim.person());
        audit.record("CLAIM_PROOF_ADDED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " proofs=" + (claim.proofs() + 1));
    }
}
