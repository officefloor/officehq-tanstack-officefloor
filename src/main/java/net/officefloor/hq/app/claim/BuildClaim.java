package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class BuildClaim {
    public void build(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val ClaimInput input, Claims claims, Audit audit) {
        claims.add(sheet, person.id(), input);
        audit.record("CLAIM_ADDED sheet=" + sheet + " by=" + user + " " + input.auditFields(person.name()));
    }
}
