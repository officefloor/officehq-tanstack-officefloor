package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the comment's body once, publishing its text; refuses (400) an empty comment. */
public class ValidateCommentRequest {
    public void validate(@RequestBody CommentRequest request, Out<CommentText> comment) throws InvalidClaimException {
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new InvalidClaimException("Type a comment.");
        }
        comment.set(new CommentText(text));
    }
}
