package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import jakarta.servlet.http.HttpServletRequest;

/**
 * Records the saved claim (with its reference, then its tags, at the end when it has them), then LATE_ENTRY naming it when it was added while its period was
 * soft-closed. Runs after every rule has passed, so a refused change writes nothing.
 */
public class AuditClaimAdded {
    public void audit(@Val Sheet sheet, @Val Person person, @Val Claim claim,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_ADDED", sheet, Audit.user(request), person, claim)
                + (claim.getRef() == null ? "" : " ref=" + claim.getRef())
                + (claim.getTags() == null ? "" : " tags=" + claim.getTags()));
        if (claim.isLateEntry()) audit.record(ClaimAudit.line("LATE_ENTRY", sheet, Audit.user(request), person, claim));
    }
}
