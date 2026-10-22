package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and a comment that is not empty. */
public class ValidateComment {
    public void validate(@RequestBody CommentRequest request, Out<CommentInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        if (request.text() == null || request.text().isBlank()) {
            throw new InvalidClaimException("Write the comment before adding it.");
        }
        body.set(new CommentInput(request.user().trim(), request.text().trim()));
    }
}
