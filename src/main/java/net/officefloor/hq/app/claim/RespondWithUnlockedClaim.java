package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithUnlockedClaim {
    public void respond(@Val Claim claim, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim));
    }
}
