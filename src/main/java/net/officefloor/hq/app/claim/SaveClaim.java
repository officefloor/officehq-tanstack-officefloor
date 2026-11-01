package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SaveClaim {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Claim claim,
            @Val ClaimInput input, Claims claims, People people, Audit audit) {
        claims.update(sheet, claim.id(), input);
        String person = people.find(sheet, claim.person()).name();
        audit.record("CLAIM_EDITED sheet=" + sheet + " by=" + user + " " + input.auditFields(person));
    }
}
