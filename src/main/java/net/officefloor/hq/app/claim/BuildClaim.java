package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class BuildClaim {
    public void build(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val ClaimInput input, Claims claims, Periods periods, Audit audit, Rollup rollup)
            throws InvalidClaimException {
        if (periods.closedOn(sheet, input.date())) {
            throw new InvalidClaimException("A claim may not be dated inside a closed period");
        }
        if (person.retiredBefore(input.date())) {
            throw new InvalidClaimException("A claim may not be dated after the person's retirement date");
        }
        input.checkCorrects(sheet, person.id(), claims);
        Rollup.Snapshot before = rollup.take(sheet);
        claims.add(sheet, person.id(), input);
        audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
        rollup.record(sheet, user, before);
    }
}
