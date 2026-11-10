package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/**
 * Records the split: the original with its remaining amount, ending with the new claim's number,
 * then the new claim's opening. Runs after every rule has passed, so a refused split writes nothing.
 */
public class AuditClaimSplit {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val Split split, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        String user = Audit.user(request);
        audit.record(ClaimAudit.line("CLAIM_SPLIT", sheet, user, people.find(sheet.id(), claim.getPersonId()), claim)
                + " into=" + split.made().getNumber());
        audit.record(AuditClaimStatus.line(sheet, split.made(), user, people));
    }
}
