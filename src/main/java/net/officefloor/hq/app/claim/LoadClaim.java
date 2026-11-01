package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;

public class LoadClaim {
    public void load(@PathVariable("sheet") String sheet, @PathVariable("claim") long claim, Claims claims,
            Out<Claim> loaded) throws ClaimNotFoundException {
        Claim found = claims.find(sheet, claim);
        if (found == null) {
            throw new ClaimNotFoundException(claim);
        }
        loaded.set(found);
    }
}
