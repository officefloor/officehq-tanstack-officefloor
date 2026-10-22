package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val ClaimResponse claim, ObjectResponse<ClaimResponse> response) {
        response.send(claim);
    }
}
