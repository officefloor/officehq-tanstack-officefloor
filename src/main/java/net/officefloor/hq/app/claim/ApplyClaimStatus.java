package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the status the change takes it to, refusing a change its status does not allow. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("sheet") String sheet, @PathVariable("change") String change,
            @RequestParam("user") String user, @Val Claim claim, Claims claims, People people, Audit audit)
            throws InvalidClaimStatusChangeException {
        ClaimStatusChange requested = ClaimStatusChange.of(change);
        if (requested == null) {
            throw new InvalidClaimStatusChangeException("No such status change: " + change);
        }
        ClaimStatus next = claim.status().after(requested);
        // Conditional on the status read, so a concurrent change by another user is refused, not overwritten.
        if (next == null || !claims.changeStatus(sheet, claim.id(), claim.status(), next)) {
            throw new InvalidClaimStatusChangeException(
                    "A " + claim.status() + " claim does not allow " + requested.name().toLowerCase());
        }
        // Opening gives the sheet's next number; voiding clears it (the line carries the number it had).
        Claim changed = claim;
        if (requested == ClaimStatusChange.OPEN) {
            changed = claim.withNumber(claims.nextNumber(sheet));
            claims.setNumber(sheet, claim.id(), changed.number());
        } else if (requested == ClaimStatusChange.VOID && claim.number() != null) {
            claims.setNumber(sheet, claim.id(), null);
        }
        String person = people.find(sheet, claim.person()).name();
        audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + changed.statusAuditFields(person));
    }
}
