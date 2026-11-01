package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Moves a DRAFT, OPEN or PENDING claim dated outside a closed period (which may not be edited) to another person on the sheet; only a role the permission matrix lets (by default a steward) may. The claim keeps its number
 * and date, loses any figure a statement bound it to, and lists as its new person's newest claim.
 */
public class ReassignClaim {
    public void reassign(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ReassignRequest body, @Val Claim claim, Claims claims, People people, Roles roles, Settings settings, Periods periods, Audit audit,
            Rollup rollup) throws InvalidClaimStatusChangeException {
        if (!settings.permits(sheet, "reassign", roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Your role may not reassign a claim");
        }
        if (claim.status() != ClaimStatus.DRAFT && claim.status() != ClaimStatus.OPEN
                && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be reassigned");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.date()))) {
            throw new InvalidClaimStatusChangeException("A claim dated inside a closed period cannot be reassigned");
        }
        Person from = people.find(sheet, claim.person());
        Person to = people.find(sheet, body.to());
        if (to == null || to.id() == claim.person()) {
            throw new InvalidClaimStatusChangeException("A claim is reassigned to another person on the sheet");
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
