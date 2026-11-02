package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward waives an OPEN or PENDING claim's late charge: from then on it bears none, whatever its overdue state.
 * Refuses anyone else, any other status, and a claim already waived.
 */
public class WaiveClaim {
    public void waive(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, Roles roles, Audit audit, Rollup rollup)
            throws InvalidClaimStatusChangeException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward may waive a claim's late charge");
        }
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim's late charge cannot be waived");
        }
        if (claim.waived()) {
            throw new InvalidClaimStatusChangeException("The claim's late charge is already waived");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the status read and on it not yet being waived, so a concurrent change is refused.
        if (!claims.waive(sheet, claim.id(), claim.status())) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("CLAIM_WAIVED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name()));
        rollup.record(sheet, user, before);
    }
}
