package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignRequest> body) throws InvalidClaimException {
        if (request == null || request.personId() == null) {
            throw new InvalidClaimException("Choose the person to move the claim to.");
        }
        body.set(request);
    }
}
