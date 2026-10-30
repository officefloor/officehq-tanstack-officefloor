package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.hq.app.voidreason.VoidReasons;

public class ValidateClaimStatus {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ClaimStatusRequest request, Out<ValidClaimStatus> body, Settings settings,
            VoidReasons voidReasons)
            throws InvalidClaimException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String action = request.action() == null ? "" : request.action().trim();
        String reason = request.reason() == null ? "" : request.reason().trim();
        ValidClaimStatus valid = switch (action) {
            case "open" -> new ValidClaimStatus(ClaimStatus.OPEN, request.user(), ValidClaimStatus.Decision.NONE, null);
            case "settle" -> new ValidClaimStatus(ClaimStatus.SETTLED, request.user(), ValidClaimStatus.Decision.NONE, null);
            case "void" -> {
                // While the sheet has void reasons, a void needs one chosen from them; without any, none is kept.
                if (voidReasons.list(sheetId).isEmpty()) {
                    yield new ValidClaimStatus(ClaimStatus.VOID, request.user(), ValidClaimStatus.Decision.NONE, null);
                }
                if (reason.isEmpty() || !voidReasons.contains(sheetId, reason)) {
                    throw new InvalidClaimException("A void needs a reason from the sheet's list");
                }
                yield new ValidClaimStatus(ClaimStatus.VOID, request.user(), ValidClaimStatus.Decision.NONE, reason);
            }
            case "delete" -> new ValidClaimStatus(ClaimStatus.DELETED, request.user(), ValidClaimStatus.Decision.NONE, null);
            case "restore" -> new ValidClaimStatus(ClaimStatus.DRAFT, request.user(), ValidClaimStatus.Decision.NONE, null);
            case "approve" -> new ValidClaimStatus(ClaimStatus.SETTLED, request.user(), ValidClaimStatus.Decision.APPROVE, null);
            case "reject" -> {
                if (reason.isEmpty()) throw new InvalidClaimException("A rejection needs a reason");
                yield new ValidClaimStatus(ClaimStatus.OPEN, request.user(), ValidClaimStatus.Decision.REJECT, reason);
            }
            default -> throw new InvalidClaimException("No such status change: " + action);
        };
        body.set(valid);
    }
}
