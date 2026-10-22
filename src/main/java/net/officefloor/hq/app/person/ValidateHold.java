package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user. */
public class ValidateHold {
    public void validate(@RequestBody HoldRequest request, Out<HoldRequest> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before holding people.");
        }
        body.set(new HoldRequest(request.user().trim()));
    }
}
