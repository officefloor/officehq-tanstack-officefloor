package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Keeps the comment on the claim, with the user who wrote it and the sheet's today. */
public class AddComment {
    public void apply(@Val CommentRequest comment, @Val Sheet sheet, @Val Claim claim,
            ClaimCommentRepository comments, HttpServletRequest request) {
        comments.add(claim, Audit.user(request), sheet.today(), comment.text());
    }
}
