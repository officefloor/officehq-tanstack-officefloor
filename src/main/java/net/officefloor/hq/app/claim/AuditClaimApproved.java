package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the approval; the line ends with the due it was settled at (n/a when its grade had no rate). */
public class AuditClaimApproved {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_APPROVED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " due=" + (claim.getSettledDue() == null ? "n/a" : claim.getSettledDue().toPlainString()));
    }
}
