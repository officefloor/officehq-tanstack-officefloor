package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Records the claim's new status; a settled claim's line ends with the due it was settled at (n/a
 * when its grade had no rate).
 * Runs after every rule has passed, so a refused change writes nothing.
 */
public class AuditClaimStatus {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(line(sheet, claim, Audit.user(request), people));
    }

    /** The claim's status line as the given user changed it. */
    static String line(Sheet sheet, Claim claim, String user, PersonRepository people) {
        String line = ClaimAudit.line(claim.getStatus().auditEvent(), sheet, user,
                people.find(sheet.id(), claim.getPersonId()), claim);
        if (claim.getStatus() == ClaimStatus.SETTLED) {
            line += " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        return line;
    }
}
