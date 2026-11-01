package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

public class SaveClaim {
    public void save(@PathVariable("sheet") String sheet, @Val Claim claim, @Val ClaimInput input, Claims claims) {
        claims.update(sheet, claim.id(), input);
    }
}
