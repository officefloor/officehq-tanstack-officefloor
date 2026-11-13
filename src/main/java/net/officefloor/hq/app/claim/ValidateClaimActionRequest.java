package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the status change's body once, trimming its reason, for later steps. */
public class ValidateClaimActionRequest {
    public void validate(@RequestBody ClaimActionRequest request, Out<ClaimActionRequest> body) {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        body.set(new ClaimActionRequest(reason));
    }
}
