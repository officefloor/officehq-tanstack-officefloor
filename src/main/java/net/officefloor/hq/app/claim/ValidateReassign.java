package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignRequest> body) throws InvalidClaimException {
        if (request.toPersonId() == null || request.toPersonId().isBlank()) {
            throw new InvalidClaimException("No person chosen to move the claim to");
        }
        body.set(request);
    }
}
