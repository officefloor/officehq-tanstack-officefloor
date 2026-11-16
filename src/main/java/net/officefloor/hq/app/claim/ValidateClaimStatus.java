package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the action is open, settle, approve, reject (which needs a reason), void or resettle. */
public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusChange> change)
            throws InvalidClaimStatusException {
        String action = request == null || request.action() == null ? "" : request.action().trim();
        ClaimAction taken = switch (action) {
            case "open" -> ClaimAction.OPEN;
            case "settle" -> ClaimAction.SETTLE;
            case "approve" -> ClaimAction.APPROVE;
            case "reject" -> ClaimAction.REJECT;
            case "void" -> ClaimAction.VOID;
            case "resettle" -> ClaimAction.RESETTLE;
            default -> throw new InvalidClaimStatusException("The action must be open, settle, approve, reject, void or resettle");
        };
        ClaimStatus to = switch (taken) {
            case OPEN, REJECT -> ClaimStatus.OPEN;
            case SETTLE, APPROVE, RESETTLE -> ClaimStatus.SETTLED;
            case VOID -> ClaimStatus.VOID;
        };
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (taken == ClaimAction.REJECT && reason.isEmpty()) {
            throw new InvalidClaimStatusException("A rejection needs a reason");
        }
        change.set(new ClaimStatusChange(taken, to, request.by() == null ? "" : request.by().trim(),
                taken == ClaimAction.REJECT ? reason : ""));
    }
}
