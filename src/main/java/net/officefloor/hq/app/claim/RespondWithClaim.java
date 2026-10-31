package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, Figures figures, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim, figures));
    }
}
