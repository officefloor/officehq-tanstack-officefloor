package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the user as the claim's last editor (four eyes: they cannot then settle or approve it). */
public class MarkClaimEdited {
    public void mark(@Val Claim claim, @RequestParam("user") String user, ClaimRepository claims) {
        claims.markEdited(claim.sheetId(), claim.id(), user);
    }
}
