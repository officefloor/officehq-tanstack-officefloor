package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Records the claim's new status; a settled claim's line ends with the due it was settled at (n/a
 * when its grade had no rate); a voided claim that had been settled ends with the frozen figure
 * the void reversed out of every total, then the reason it was voided with, when it had one.
 * Runs after every rule has passed, so a refused change writes nothing.
 */
public class AuditClaimStatus {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people, ClaimLogRepository log,
            HttpServletRequest request, Audit audit) {
        String line = line(sheet, claim, Audit.user(request), people);
        if (claim.getStatus() == ClaimStatus.VOID && log.settledOn(claim) != null) {
            line += " reversed=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString());
        }
        if (claim.getStatus() == ClaimStatus.VOID && claim.getVoidReason() != null) {
            line += " reason=" + claim.getVoidReason();
        }
        audit.record(line);
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
