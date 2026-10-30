package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.plugin.variable.Val;

public class AuditClaimEdited {
    public void audit(@Val ValidClaim request, @Val Claim claim, People people, Audit audit) {
        audit.recordOnCommit(ClaimAudit.line("CLAIM_EDITED", request.user(),
                people.find(claim.getSheetId(), claim.getPersonId()), claim));
    }
}
