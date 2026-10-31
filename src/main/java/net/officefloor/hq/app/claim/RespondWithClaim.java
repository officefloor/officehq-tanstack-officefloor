package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, Figures figures, ClaimLog log, ClaimComments comments, ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim, figures, log.of(claim), comments.of(claim)));
    }
}
