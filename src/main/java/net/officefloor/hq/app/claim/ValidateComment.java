package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the comment's text (trimmed; a blank one is refused) and who writes it (a blank name is refused). */
public class ValidateComment {
    public void validate(@RequestBody CommentRequest request, Out<ClaimComment> comment) throws InvalidClaimException {
        String by = request == null || request.by() == null ? "" : request.by().trim();
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (by.isEmpty()) {
            throw new InvalidClaimException("Who comments the claim is required");
        }
        if (text.isEmpty()) {
            throw new InvalidClaimException("A comment cannot be empty");
        }
        comment.set(new ClaimComment(by, null, text));
    }
}
