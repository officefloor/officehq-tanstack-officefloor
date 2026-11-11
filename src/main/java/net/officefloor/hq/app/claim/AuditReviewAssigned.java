package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the review assignment, naming the claim; the line ends with the user it is assigned to. */
public class AuditReviewAssigned {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("REVIEW_ASSIGNED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim) + " user=" + claim.getReviewUser());
    }
}
