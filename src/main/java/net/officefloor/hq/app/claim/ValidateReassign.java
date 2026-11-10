package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A move names the person it moves the claim to; it is kept trimmed. */
public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignRequest> body) throws InvalidClaimException {
        String personId = request == null || request.personId() == null ? "" : request.personId().trim();
        if (personId.isEmpty()) throw new InvalidClaimException("A move needs the person it moves the claim to");
        body.set(new ReassignRequest(personId));
    }
}
