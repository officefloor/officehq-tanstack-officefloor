package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the waiver of the claim's late charge, naming the claim like every claim line. */
public class AuditClaimWaived {
    public void audit(@Val Sheet sheet, @Val Claim claim, PersonRepository people, HttpServletRequest request,
            Audit audit) {
        Person person = people.find(sheet.id(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_WAIVED", sheet, Audit.user(request), person, claim));
    }
}
