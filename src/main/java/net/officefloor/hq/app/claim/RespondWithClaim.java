package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, ObjectResponse<ClaimResponse> response, ClaimRepository claims, ClaimLogRepository log) {
        response.send(ClaimResponse.from(claim, claims, log));
    }
}
