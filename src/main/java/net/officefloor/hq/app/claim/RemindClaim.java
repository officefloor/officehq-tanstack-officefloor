package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reminds an OPEN or PENDING claim: REMIND is written naming the claim, and the claim keeps the sheet's today as its
 * reminded date. A claim is reminded at most once per sheet-today.
 */
public class RemindClaim {
    public void remind(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, SheetClock clock, Audit audit) throws InvalidClaimStatusChangeException {
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be reminded");
        }
        LocalDate today = clock.today(sheet);
        if (today.toString().equals(claim.remindedOn())) {
            throw new InvalidClaimStatusChangeException("A claim is reminded once per sheet-today");
        }
        Person person = people.find(sheet, claim.person());
        // Conditional on the status read and the day, so a concurrent reminder or change by another user is refused.
        if (!claims.remind(sheet, claim.id(), claim.status(), today)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("REMIND sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name()));
    }
}
