package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A rejection needs a reason; it is kept trimmed. */
public class ValidateReject {
    public void validate(@RequestBody RejectRequest request, Out<RejectRequest> body) throws InvalidClaimException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) throw new InvalidClaimException("A rejection needs a reason");
        body.set(new RejectRequest(reason));
    }
}
