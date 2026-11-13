package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the status change's body once, trimming its reason and batch token, for later steps. */
public class ValidateClaimActionRequest {
    public void validate(@RequestBody ClaimActionRequest request, Out<ClaimActionRequest> body) {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        String batch = request == null || request.batch() == null ? "" : request.batch().trim();
        body.set(new ClaimActionRequest(reason, batch));
    }
}
