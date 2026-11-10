package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the re-dating, naming the claim with its new date; the line ends with its old and new dates. */
public class AuditClaimRedated {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val Redating redating, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_REDATED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " old=" + redating.from() + " new=" + redating.to());
    }
}
