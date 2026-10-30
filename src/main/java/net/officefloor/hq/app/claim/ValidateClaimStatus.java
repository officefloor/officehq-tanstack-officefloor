package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ValidClaimStatus> body)
            throws InvalidClaimException {
        String action = request.action() == null ? "" : request.action().trim();
        ClaimStatus target = switch (action) {
            case "open" -> ClaimStatus.OPEN;
            case "settle" -> ClaimStatus.SETTLED;
            case "void" -> ClaimStatus.VOID;
            default -> throw new InvalidClaimException("No such status change: " + action);
        };
        body.set(new ValidClaimStatus(target, request.user()));
    }
}
