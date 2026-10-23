package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, a known action, and a reason for a
 *  reject (a resettle freezes a SETTLED claim again; a delete makes a DRAFT DELETED, a restore a DELETED one a DRAFT again). A void's reason, chosen from the sheet's list, is kept
 *  (blank: none) and checked once the claim is loaded. */
public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusInput> body)
            throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String action = request.action() == null ? "" : request.action().trim();
        ClaimStatus to = switch (action) {
            case "open", "reject", "review" -> ClaimStatus.OPEN;
            case "settle", "approve", "resettle" -> ClaimStatus.SETTLED;
            case "void" -> ClaimStatus.VOID;
            case "delete" -> ClaimStatus.DELETED;
            case "restore" -> ClaimStatus.DRAFT;
            default -> throw new InvalidClaimException("Unknown claim action: " + action + ".");
        };
        String reason = null;
        if (action.equals("reject")) {
            reason = request.reason() == null ? "" : request.reason().trim();
            if (reason.isEmpty()) {
                throw new InvalidClaimException("Give a reason to reject a claim.");
            }
        } else if (action.equals("void") && request.reason() != null && !request.reason().isBlank()) {
            reason = request.reason().trim();
        }
        java.time.LocalDate on = null;
        if (action.equals("settle") && request.on() != null && !request.on().isBlank()) {
            try {
                on = java.time.LocalDate.parse(request.on().trim());
            } catch (java.time.format.DateTimeParseException e) {
                throw new InvalidClaimException("Give the settle date as yyyy-mm-dd.");
            }
        }
        body.set(new ClaimStatusInput(request.user().trim(), action, to, reason, on));
    }
}
