package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward moves an OPEN or PENDING claim to another date, on or before its person's retirement, unless it is due
 * inside a closed period. Its due-by stays as it was fixed and a figure its statement bound stays bound; so does its
 * period (the one its due-by falls in), and with it any number it has within its period.
 */
public class RedateClaim {
    public void redate(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val LocalDate date,
            @Val Claim claim, Claims claims, People people, Periods periods, Roles roles, Audit audit,
            Rollup rollup) throws InvalidClaimStatusChangeException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidClaimStatusChangeException("Only a steward may re-date a claim");
        }
        if (claim.status() != ClaimStatus.OPEN && claim.status() != ClaimStatus.PENDING) {
            throw new InvalidClaimStatusChangeException("A " + claim.status() + " claim cannot be re-dated");
        }
        LocalDate old = LocalDate.parse(claim.date());
        if (periods.closedOn(sheet, LocalDate.parse(claim.dueBy()))) {
            throw new InvalidClaimStatusChangeException("A claim due inside a closed period cannot be re-dated");
        }
        Person person = people.find(sheet, claim.person());
        if (person.retiredBefore(date)) {
            throw new InvalidClaimStatusChangeException("A claim cannot be dated after its person's retirement");
        }
        Rollup.Snapshot before = rollup.take(sheet);
        // Conditional on the status and date read, so a concurrent change by another user is refused, not overwritten.
        if (!claims.redate(sheet, claim.id(), claim.status(), old, date)) {
            throw new InvalidClaimStatusChangeException("The claim changed since it was read");
        }
        Claim redated = claim.withDate(date.toString());
        audit.record("CLAIM_REDATED sheet=" + sheet + " by=" + user + " " + redated.statusAuditFields(person.name())
                + " old=" + old + " new=" + date);
        rollup.record(sheet, user, before);
    }
}
