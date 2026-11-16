package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the action is open, settle or void. */
public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusChange> change)
            throws InvalidClaimStatusException {
        String action = request == null || request.action() == null ? "" : request.action().trim();
        ClaimStatus to = switch (action) {
            case "open" -> ClaimStatus.OPEN;
            case "settle" -> ClaimStatus.SETTLED;
            case "void" -> ClaimStatus.VOID;
            default -> throw new InvalidClaimStatusException("The action must be open, settle or void");
        };
        change.set(new ClaimStatusChange(to, request.by() == null ? "" : request.by().trim()));
    }
}
