package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and a person to move the claim to. */
public class ValidateReassign {
    public void validate(@RequestBody ReassignRequest request, Out<ReassignInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        long personId;
        try {
            personId = Long.parseLong(request.personId() == null ? "" : request.personId().trim());
        } catch (NumberFormatException e) {
            throw new InvalidClaimException("Choose the person to move the claim to.");
        }
        body.set(new ReassignInput(request.user().trim(), personId));
    }
}
