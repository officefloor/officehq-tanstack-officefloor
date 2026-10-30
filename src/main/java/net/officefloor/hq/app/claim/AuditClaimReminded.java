package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.plugin.variable.Val;

public class AuditClaimReminded {
    public void audit(@Val RemindRequest request, @Val Claim claim, People people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.recordOnCommit(ClaimAudit.line("REMIND", request.user(), person, claim, claim.getNumber()));
        if (claim.isEscalating()) {
            audit.recordOnCommit(ClaimAudit.line("ESCALATE", request.user(), person, claim, claim.getNumber()));
        }
    }
}
