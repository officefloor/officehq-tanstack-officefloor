package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithClaim {
    public void respond(@Val Claim claim, ClaimLogRepository log, ClaimCommentRepository comments,
            ObjectResponse<ClaimResponse> response) {
        response.send(ClaimResponse.from(claim, log.linesOf(claim), comments.linesOf(claim)));
    }
}
