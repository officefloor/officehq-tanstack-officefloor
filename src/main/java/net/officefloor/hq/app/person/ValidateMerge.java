package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a person to merge into. */
public class ValidateMerge {
    public void validate(@RequestBody MergeRequest request, Out<MergeInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before merging people.");
        }
        if (request.intoId() == null) {
            throw new InvalidPersonException("Choose a person to merge into.");
        }
        body.set(new MergeInput(request.user().trim(), request.intoId()));
    }
}
