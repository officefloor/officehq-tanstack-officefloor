package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to another person on the sheet by the rules of {@link ClaimReassigner}. */
public class ReassignClaim {
    public void reassign(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @Val ReassignRequest body, @Val Claim claim, ClaimReassigner reassigner)
            throws InvalidClaimStatusChangeException {
        reassigner.reassign(sheet, user, claim, body.to(), false);
    }
}
