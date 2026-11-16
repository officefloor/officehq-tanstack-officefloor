package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a move names the person the claim moves to. */
public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignRequest> body)
            throws InvalidClaimStatusException {
        if (request == null || request.personId() == null) {
            throw new InvalidClaimStatusException("A move needs the person the claim moves to");
        }
        body.set(new ReassignRequest(request.personId(), request.by() == null ? "" : request.by().trim()));
    }
}
