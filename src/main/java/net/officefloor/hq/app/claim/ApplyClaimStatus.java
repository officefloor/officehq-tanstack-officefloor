package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rate.Rates;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the status the change takes it to, refusing a change its status does not allow. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("sheet") String sheet, @PathVariable("change") String change,
            @RequestParam("user") String user, @Val Claim claim, Claims claims, People people, Rates rates, Audit audit)
            throws InvalidClaimStatusChangeException {
        ClaimStatusChange requested = ClaimStatusChange.of(change);
        if (requested == null) {
            throw new InvalidClaimStatusChangeException("No such status change: " + change);
        }
        // Four eyes: the user who opened a claim may not settle it.
        if (requested == ClaimStatusChange.SETTLE && user.equals(claim.openedBy())) {
            throw new InvalidClaimStatusChangeException("The user who opened a claim cannot settle it");
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
            claims.setOpenedBy(sheet, claim.id(), user);
        } else if (requested == ClaimStatusChange.VOID && claim.number() != null) {
            claims.setNumber(sheet, claim.id(), null);
        }
        Person person = people.find(sheet, claim.person());
        String fields = changed.statusAuditFields(person.name());
        // Settling fixes the due at the person's grade rate in force on the claim's date (or the figure its
        // statement bound); with no rate there is nothing to fix, so it stays live.
        if (requested == ClaimStatusChange.SETTLE) {
            BigDecimal due = claim.due(rates.list(sheet), person.grade());
            claims.setSettledDue(sheet, claim.id(), due);
            fields += " due=" + (due == null ? "n/a" : due.toPlainString());
        }
        audit.record(requested.event() + " sheet=" + sheet + " by=" + user + " " + fields);
    }
}
