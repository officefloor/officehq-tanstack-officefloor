package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class BuildClaim {
    public void build(@PathVariable("sheet") String sheet, @Val Person person, @Val ClaimInput input, Claims claims) {
        claims.add(sheet, person.id(), input);
    }
}
