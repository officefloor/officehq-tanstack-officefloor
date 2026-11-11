package net.officefloor.hq.app.claim;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the claim shared (CLAIM_SHARED), naming the claim like every claim line, then the holder and their share. */
public class AuditClaimShared {
    public void audit(@Val Sheet sheet, @Val Claim claim, @Val Sharing sharing, @Val Person holder,
            PersonRepository people, HttpServletRequest request, Audit audit) {
        Person person = people.find(sheet.id(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_SHARED", sheet, Audit.user(request), person, claim)
                + " with=" + holder.getName() + " share=" + sharing.share().toPlainString());
    }
}
