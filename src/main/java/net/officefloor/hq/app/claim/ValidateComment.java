package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the comment from the body, its text trimmed; refuses an empty one. */
public class ValidateComment {
    public void validate(@RequestBody CommentRequest request, Out<CommentRequest> comment) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidClaimException("A comment cannot be empty.");
        }
        comment.set(new CommentRequest(text));
    }
}
