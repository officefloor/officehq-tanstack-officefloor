package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.stereotype.Service;

/**
 * Moves a DRAFT, OPEN or PENDING claim dated outside a closed period (which may not be edited) to another person on
 * the sheet, on or before that person's retirement; only a role the permission matrix lets (by default a steward) may.
 * The claim keeps its number and date, loses any figure a statement bound it to, and lists as its new person's newest
 * claim. Shared by a claim's own Reassign button and the bulk reassign.
 */
@Service
public class ClaimReassigner {

    private final Claims claims;
    private final People people;
    private final Roles roles;
    private final Settings settings;
    private final Periods periods;
    private final Audit audit;
    private final Rollup rollup;

    public ClaimReassigner(Claims claims, People people, Roles roles, Settings settings, Periods periods, Audit audit,
            Rollup rollup) {
        this.claims = claims;
        this.people = people;
        this.roles = roles;
        this.settings = settings;
        this.periods = periods;
        this.audit = audit;
        this.rollup = rollup;
    }

    /**
     * Moves the claim to the person, writing nothing when it is refused. {@code samePerson} lets a claim already with
     * the person be moved to them again (the bulk reassign takes every selected claim to the one person).
     */
    public void reassign(String sheet, String user, Claim claim, Long toId, boolean samePerson)
            throws InvalidClaimStatusChangeException {
        if (!settings.permits(sheet, "reassign", roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Your role may not reassign a claim");
        }
        if (claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.OPEN
                && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be reassigned");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot be reassigned");
        }
        Person from = people.find(sheet, claim.person());
        Person to = toId == null ? null : people.find(sheet, toId);
        if (to == null || (!samePerson && to.id() == claim.person())) {
            throw new InvalidClaimStatusChangeException("A claim is reassigned to another person on the sheet");
        }
        if (to.retiredBefore(LocalDate.parse(claim.date()))) {
            throw new InvalidClaimStatusChangeException("A claim cannot move to a person retired before its date");
        }
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the person and status read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.reassign(sheet, claim.id(), claim.person(), claim.status(), to.id())) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("CLAIM_REASSIGNED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(to.name())
                + " from=" + from.name() + " to=" + to.name());
        rollup.record(sheet, user, before);
    }
}
