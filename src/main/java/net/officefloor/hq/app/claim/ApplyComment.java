package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.AuditUser;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Adds the comment to the claim, by the signed-in name, dated the sheet's today. A claim of any status can be
 *  commented. */
public class ApplyComment {
    public void apply(@Val CommentRequest comment, @Val Claim claim, @Val Sheet sheet,
            @RequestHeader(name = "X-User", required = false) String user, ClaimCommentRepository comments) {
        comments.saveAndFlush(new ClaimComment(claim, AuditUser.decode(user), sheet.getToday(), comment.text()));
    }
}
