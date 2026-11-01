package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveClaim {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            @Val ClaimInput input, Claims claims, People people, Periods periods, Audit audit, Rollup rollup)
            throws InvalidClaimException {
        // A claim dated inside a closed period may not be edited, nor moved into one.
        if (periods.closedOn(sheet, LocalDate.parse(claim.date())) || periods.closedOn(sheet, input.date())) {
            throw new InvalidClaimException("A claim dated inside a closed period may not be edited");
        }
        Rollup.Snapshot before = rollup.take(sheet);
        claims.update(sheet, claim.id(), input);
        String person = people.find(sheet, claim.person()).name();
        audit.record("CLAIM_EDITED sheet=" + sheet + " by=" + user + " " + input.auditFields(person));
        rollup.record(sheet, user, before);
    }
}
