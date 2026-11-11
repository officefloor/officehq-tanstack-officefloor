package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the line, naming the claim with its new amount; the line ends with the line's label, quantity and unit. */
public class AuditClaimLineAdded {
    public void audit(@Val ClaimLine line, @Val Sheet sheet, @Val Claim claim, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_LINE_ADDED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " label=" + line.label() + " qty=" + line.qty() + " unit=" + line.unit().toPlainString());
    }
}
