package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Moves the claim to the status the change takes it to, refusing a change its status does not allow. */
public class ApplyClaimStatus {
    public void apply(@PathVariable("sheet") String sheet, @PathVariable("change") String change,
            @RequestParam("user") String user, @RequestBody ClaimStatusRequest body, @Val Claim claim,
            ClaimStatusChanger changer, Claims claims) throws InvalidClaimStatusChangeException {
        ClaimStatusChange requested = ClaimStatusChange.of(change);
        if (requested == null) {
            throw new InvalidClaimStatusChangeException("No such status change: " + change);
        }
        changer.change(sheet, user, claim, requested, body == null ? null : body.reason());
        if (requested == ClaimStatusChange.SETTLE) {
            // Settled one at a time: no longer part of a bulk settle batch.
            claims.setBatch(sheet, claim.id(), null);
        }
    }
}
