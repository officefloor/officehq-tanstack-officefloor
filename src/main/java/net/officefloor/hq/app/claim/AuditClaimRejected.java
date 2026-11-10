package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the rejection; the line ends with its reason. */
public class AuditClaimRejected {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val RejectRequest reject, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REJECTED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim) + " reason=" + reject.reason());
    }
}
