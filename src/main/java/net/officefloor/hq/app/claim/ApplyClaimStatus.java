package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the status the change takes it to, refusing a change its status does not allow. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("sheet") String sheet, @PathVariable("change") String change,
            @RequestParam("user") String user, @Val Claim claim, Claims claims, People people, Rates rates, Periods periods,
            Roles roles, Settings settings, Audit audit, Rollup rollup)
            throws InvalidClaimStatusChangeException {
        ClaimStatusChange requested = ClaimStatusChange.of(change);
        if (requested == null) {
            throw new InvalidClaimStatusChangeException("No such status change: " + change);
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.date()))) {
            throw new InvalidClaimStatusChangeException("A claim dated inside a closed period cannot change status");
        }
        // Four eyes: the user who opened a claim may not settle it.
        if (requested == ClaimStatusChange.SETTLE && user.equals(claim.openedBy())) {
            throw new InvalidClaimStatusChangeException("The user who opened a claim cannot settle it");
        }
        Person person = people.find(sheet, claim.person());
        // A clerk settles only a due at or below the sheet's clerk limit (a due of n/a is within any limit);
        // a steward settles anything.
        if (requested == ClaimStatusChange.SETTLE && Role.CLERK.equals(roles.of(sheet, user))) {
            BigDecimal limit = settings.clerkLimit(sheet);
            BigDecimal due = claim.due(rates.list(sheet), person.grade(), settings.floor(sheet));
            if (limit != null && due != null && due.compareTo(limit) > 0) {
                throw new InvalidClaimStatusChangeException("A clerk cannot settle a due above the clerk limit");
            }
        }
        Rollup.Snapshot before = rollup.take(sheet);
        ClaimStatus next = claim.status().after(requested);
        // Conditional on the status read, so a concurrent change by another user is refused, not overwritten.
        if (next == null || !claims.changeStatus(sheet, claim.id(), claim.status(), next)) {
            throw new InvalidClaimStatusChangeException(
                    "A " + claim.status() + " claim does not allow " + requested.name().toLowerCase());
        }
        // Opening gives the sheet's next number; a voided claim keeps the number it had (never reused).
        Claim changed = claim;
        if (requested == ClaimStatusChange.OPEN) {
            changed = claim.withNumber(claims.nextNumber(sheet));
            claims.setNumber(sheet, claim.id(), changed.number());
            claims.setOpenedBy(sheet, claim.id(), user);
        }
        String fields = changed.statusAuditFields(person.name());
        // Settling fixes the due at the person's grade rate in force on the claim's date (or the figure its
        // statement bound); with no rate there is nothing to fix, so it stays live.
        if (requested == ClaimStatusChange.SETTLE) {
            BigDecimal due = claim.due(rates.list(sheet), person.grade(), settings.floor(sheet));
            claims.setSettledDue(sheet, claim.id(), due);
            fields += " due=" + (due == null ? "n/a" : due.toPlainString());
        }
        audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + fields);
        rollup.record(sheet, user, before);
    }
}
