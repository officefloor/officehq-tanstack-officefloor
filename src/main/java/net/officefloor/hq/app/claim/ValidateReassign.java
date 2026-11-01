package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the reassign body once, refusing one that names no person. */
public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignRequest> body)
            throws InvalidClaimStatusChangeException {
        if (request == null || request.to() == null) {
            throw new InvalidClaimStatusChangeException("A reassign names the person the claim moves to");
        }
        body.set(request);
    }
}
