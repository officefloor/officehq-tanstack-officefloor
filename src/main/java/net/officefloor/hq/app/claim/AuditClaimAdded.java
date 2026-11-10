package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/** Records the saved claim. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditClaimAdded {
    public void audit(@Val Sheet sheet, @Val Person person, @Val Claim claim,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_ADDED", sheet, Audit.user(request), person, claim));
    }
}
