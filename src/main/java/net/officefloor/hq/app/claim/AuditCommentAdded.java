package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the comment, naming the claim like every claim line; the line ends with its text. */
public class AuditCommentAdded {
    public void audit(@Val CommentRequest comment, @Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("COMMENT_ADDED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim) + " text=" + comment.text());
    }
}
