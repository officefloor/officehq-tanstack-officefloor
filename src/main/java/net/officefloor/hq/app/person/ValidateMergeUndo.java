package net.officefloor.hq.app.person;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user. */
public class ValidateMergeUndo {
    public void validate(@RequestBody MergeUndoRequest request, Out<MergeUndoRequest> body)
            throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before undoing merges.");
        }
        body.set(new MergeUndoRequest(request.user().trim()));
    }
}
