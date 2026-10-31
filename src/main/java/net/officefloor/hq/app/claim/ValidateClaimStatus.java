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
            case "settle" -> {
                // Settled as of a date when one is given; blank settles as of the sheet's today.
                String on = request.on() == null ? "" : request.on().trim();
                java.time.LocalDate asOf = null;
                if (!on.isEmpty()) {
                    try {
                        asOf = java.time.LocalDate.parse(on);
                    } catch (java.time.format.DateTimeParseException bad) {
                        throw new InvalidClaimException("Not a date to settle as of: " + on);
                    }
                }
                yield new ValidClaimStatus(ClaimStatus.SETTLED, request.user(), ValidClaimStatus.Decision.NONE, null, asOf);
            }
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
            case "review" -> new ValidClaimStatus(ClaimStatus.OPEN, request.user(), ValidClaimStatus.Decision.REVIEW, null);
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
