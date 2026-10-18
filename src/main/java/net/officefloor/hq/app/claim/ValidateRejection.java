package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rejection from the body, refusing one without a reason. */
public class ValidateRejection {
    public void validate(@RequestBody RejectRequest request, Out<Rejection> rejectionOut) throws InvalidClaimException {
        String reason = request == null || request.reason() == null ? "" : request.reason().trim();
        if (reason.isEmpty()) {
            throw new InvalidClaimException("A rejection must give a reason.");
        }
        rejectionOut.set(new Rejection(reason));
    }
}
