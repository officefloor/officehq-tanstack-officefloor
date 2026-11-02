package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Holds an OPEN or PENDING claim, or releases it when held: while held and not bound by a statement it counts in no
 * live total, as a held person's claims do. Refuses any other status.
 */
public class HoldClaim {
    public void hold(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, Audit audit, Rollup rollup) throws InvalidClaimStatusChangeException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be held or released");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        boolean held = !claim.held();
        // Conditional on the status and held flag read, so a concurrent change is refused.
        if (!claims.hold(sheet, claim.id(), claim.status(), held)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record((held ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + sheet + " by=" + user + " "
                + claim.statusAuditFields(person.name()));
        rollup.record(sheet, user, before);
    }
}
