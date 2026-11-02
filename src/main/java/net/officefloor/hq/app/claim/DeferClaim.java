package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Defers an OPEN claim dated outside a closed period until a date after the sheet's today: until the sheet's today
 * reaches it the claim counts in no total, is not overdue, cannot be settled and is left unbound by a statement. A claim a
 * statement binds stays in the totals and counts, and can be settled, while bound: the deferral takes effect only once
 * the binding ends (the statement is cancelled, or the next one leaves it unbound).
 */
public class DeferClaim {
    public void defer(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate until,
            @Val Claim claim, Claims claims, People people, Periods periods, SheetClock clock, Audit audit,
            Rollup rollup) throws InvalidClaimStatusChangeException {
        if (claim.status() != ClaimStatus.OPEN) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be deferred");
        }
        if (!until.isAfter(clock.today(sheet))) {
            throw new InvalidClaimStatusChangeException("A claim is deferred to a date after the sheet's today");
        }
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot be deferred");
        }
        Person person = people.find(sheet, claim.person());
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the status read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.defer(sheet, claim.id(), until)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        audit.record("CLAIM_DEFERRED sheet=" + sheet + " by=" + user + " " + claim.statusAuditFields(person.name())
                + " until=" + until);
        rollup.record(sheet, user, before);
    }
}
