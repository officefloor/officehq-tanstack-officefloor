package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** The user commenting the claim, and the comment's text: an empty comment is refused. */
public class ValidateComment {
    public void validate(@RequestBody CommentRequest request, Out<ChangedBy> changedBy, Out<ClaimComment> comment)
            throws BadRequestException {
        changedBy.set(ChangedBy.of(request == null ? null : request.by()));
        String text = request == null || request.text() == null ? "" : request.text().trim();
        if (text.isEmpty()) {
            throw new BadRequestException("Enter a comment");
        }
        if (text.length() > 4000) {
            throw new BadRequestException("A comment is at most 4000 characters");
        }
        comment.set(new ClaimComment(text));
    }
}
