package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the action is open, settle, approve, reject (which needs a reason), review, void (with the void reason
 *  chosen, if any), resettle, delete or restore; a settle may be made as of a date. */
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
            case "review" -> ClaimAction.REVIEW;
            case "delete" -> ClaimAction.DELETE;
            case "restore" -> ClaimAction.RESTORE;
            default -> throw new InvalidClaimStatusException("The action must be open, settle, approve, reject, void, resettle, delete, restore or review");
        };
        ClaimStatus to = switch (taken) {
            case OPEN, REJECT, REVIEW -> ClaimStatus.OPEN;
            case SETTLE, APPROVE, RESETTLE -> ClaimStatus.SETTLED;
            case VOID -> ClaimStatus.VOID;
            case DELETE -> ClaimStatus.DELETED;
            case RESTORE -> ClaimStatus.DRAFT;
        };
        String reason = request.reason() == null ? "" : request.reason().trim();
        if (taken == ClaimAction.REJECT && reason.isEmpty()) {
            throw new InvalidClaimStatusException("A rejection needs a reason");
        }
        String onText = taken != ClaimAction.SETTLE || request.on() == null ? "" : request.on().trim();
        java.time.LocalDate on = null;
        if (!onText.isEmpty()) {
            on = net.officefloor.hq.app.sheet.AsOf.parse(onText);
            if (on == null) {
                throw new InvalidClaimStatusException("A settle's as-of date must be a date (yyyy-MM-dd)");
            }
        }
        change.set(new ClaimStatusChange(taken, to, request.by() == null ? "" : request.by().trim(),
                taken == ClaimAction.REJECT || taken == ClaimAction.VOID ? reason : "", on));
    }
}
