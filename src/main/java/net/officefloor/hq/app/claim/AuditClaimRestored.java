package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the deleted claim made a DRAFT again. Runs after every rule has passed, so a refused restore writes nothing. */
public class AuditClaimRestored {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people, HttpServletRequest request,
            Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_RESTORED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim));
    }
}
