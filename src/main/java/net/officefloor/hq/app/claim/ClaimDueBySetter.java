package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import org.springframework.stereotype.Service;

/**
 * A steward sets a DRAFT, OPEN or PENDING claim's due-by directly, unless it is due, or would fall due, inside a closed
 * period. A due-by set so stays as set (a holiday added later does not move it); overdue, the late charge, grace and
 * the claim's period follow it.
 */
@Service
public class ClaimDueBySetter {

    private final Claims claims;
    private final People people;
    private final Periods periods;
    private final Roles roles;
    private final Audit audit;
    private final Rollup rollup;

    public ClaimDueBySetter(Claims claims, People people, Periods periods, Roles roles, Audit audit, Rollup rollup) {
        this.claims = claims;
        this.people = people;
        this.periods = periods;
        this.roles = roles;
        this.audit = audit;
        this.rollup = rollup;
    }

    /** Sets the claim's due-by, writing nothing when it is refused. */
    public void set(String sheet, String user, Claim claim, LocalDate dueBy) throws InvalidClaimStatusChangeException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward may set a claim's due-by");
        }
        if (claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.OPEN
                && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim's due-by cannot be set");
        }
        LocalDate old = LocalDate.parse(claim.dueBy());
        if (periods.closedOn(sheet, old) || periods.closedOn(sheet, dueBy)) {
            throw new InvalidClaimStatusChangeException("A due-by inside a closed period cannot be set");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the status and due-by read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.setDueBy(sheet, claim.id(), claim.status(), old, dueBy)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("CLAIM_DUE_BY_SET sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " old=" + old + " new=" + dueBy);
        rollup.record(sheet, user, before);
    }
}
