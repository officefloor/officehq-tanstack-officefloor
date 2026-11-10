package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the claim's new status. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditClaimStatus {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line(claim.getStatus().auditEvent(), sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim));
    }
}
