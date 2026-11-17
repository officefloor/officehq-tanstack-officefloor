package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: whether the claim is to be held (a missing value holds it) and who does it. */
public class ValidateClaimHold {
    public void validate(@RequestBody ClaimHoldRequest request, Out<ClaimHold> hold) {
        hold.set(new ClaimHold(request == null || request.held() == null || request.held(),
                request == null || request.by() == null ? "" : request.by().trim()));
    }
}
