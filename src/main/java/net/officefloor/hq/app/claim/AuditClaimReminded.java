package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the reminder, then its escalation when it escalated the claim, naming the claim like every claim line. */
public class AuditClaimReminded {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val ClaimEscalation escalation, PersonRepository people,
            HttpServletRequest request, Audit audit) {
        String user = Audit.user(request);
        Person person = people.find(sheet.id(), claim.getPersonId());
        audit.record(ClaimAudit.line("REMIND", sheet, user, person, claim));
        if (escalation.escalated()) audit.record(ClaimAudit.line("ESCALATE", sheet, user, person, claim));
    }
}
