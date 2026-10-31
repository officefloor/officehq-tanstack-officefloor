package net.officefloor.hq.app.claim;

import java.util.Locale;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.rollup.Actor;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimStatus {
    public void validate(@RequestBody ClaimStatusRequest request, Out<ClaimStatusChange> body, Out<Actor> actor)
            throws InvalidSheetRequestException {
        String action = request == null || request.action() == null ? "" : request.action().trim();
        try {
            actor.set(new Actor(request.user()));
            ClaimAction parsed = ClaimAction.valueOf(action.toUpperCase(Locale.ROOT));
            String reason = request.reason() == null ? "" : request.reason().trim();
            if (parsed == ClaimAction.REJECT && reason.isEmpty()) {
                throw new InvalidSheetRequestException("A rejection needs a reason");
            }
            body.set(new ClaimStatusChange(parsed, request.user(), parsed == ClaimAction.REJECT || (parsed == ClaimAction.VOID && !reason.isEmpty()) ? reason : null));
        } catch (IllegalArgumentException e) {
            throw new InvalidSheetRequestException("Not a claim action (open, settle, approve, reject or void): " + action);
        }
    }
}
