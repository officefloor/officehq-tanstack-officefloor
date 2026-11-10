package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the reminder, naming the claim like every claim line. */
public class AuditClaimReminded {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("REMIND", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim));
    }
}
