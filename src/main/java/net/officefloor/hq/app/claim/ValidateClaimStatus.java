package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and a known action. */
public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusInput> body)
            throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String action = request.action() == null ? "" : request.action().trim();
        ClaimStatus to = switch (action) {
            case "open" -> ClaimStatus.OPEN;
            case "settle" -> ClaimStatus.SETTLED;
            case "void" -> ClaimStatus.VOID;
            default -> throw new InvalidClaimException("Unknown claim action: " + action + ".");
        };
        body.set(new ClaimStatusInput(request.user().trim(), to));
    }
}
