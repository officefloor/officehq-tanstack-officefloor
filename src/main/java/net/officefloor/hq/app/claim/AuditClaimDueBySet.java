package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the due-by set directly, naming the claim; the line ends with its old and new due-by. */
public class AuditClaimDueBySet {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val DueBySetting setting, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_DUE_BY_SET", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " old=" + setting.from() + " new=" + setting.to());
    }
}
