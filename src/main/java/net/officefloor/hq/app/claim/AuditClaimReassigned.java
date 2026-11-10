package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the move, naming the claim with its new person; the line ends with who it moved from and to. */
public class AuditClaimReassigned {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val Reassignment reassignment, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REASSIGNED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " from=" + reassignment.from() + " to=" + reassignment.to());
    }
}
