package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the re-settle, naming the claim; the line ends with its old and new due (n/a for none). */
public class AuditClaimResettled {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val Resettlement resettlement, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        audit.record(ClaimAudit.line("CLAIM_RESETTLED", sheet, Audit.user(request),
                people.find(sheet.id(), claim.getPersonId()), claim)
                + " old=" + shown(resettlement.old()) + " new=" + shown(resettlement.now()));
    }

    private static String shown(BigDecimal due) {
        return due == null ? "n/a" : due.toPlainString();
    }
}
