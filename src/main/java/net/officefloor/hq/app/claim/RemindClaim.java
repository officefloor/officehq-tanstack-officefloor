package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Reminds an OPEN or PENDING claim: REMIND is written naming the claim, and the claim keeps the sheet's today as its
 * reminded date. A claim is reminded at most once per sheet-today. While the sheet has an escalation gap, reminding a
 * claim whose previous reminder was at least that many days before the sheet's today escalates it: ESCALATE naming the
 * claim is written after the REMIND line, once per claim ever.
 */
public class RemindClaim {
    public void remind(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            Claims claims, People people, Settings settings, SheetClock clock, Audit audit) throws InvalidClaimStatusChangeException {
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
        Integer gap = settings.escalateAfter(sheet);
        if (gap != null && claim.remindedOn() != null
                && !LocalDate.parse(claim.remindedOn()).plusDays(gap).isAfter(today)
                && claims.escalate(sheet, claim.id())) {
            audit.record("ESCALATE sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name()));
        }
    }
}
