package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** REMIND, naming the claim; then ESCALATE, naming it, when the reminder escalated it. */
public class RecordClaimReminded {
    public void record(@Val Claim claim, @Val ChangedBy by, @Val RemindEscalation escalation, PersonRepository people,
            Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String name = person == null ? "" : person.getName();
        audit.record(ClaimAudit.line("REMIND", claim, by, name));
        if (escalation != null && escalation.escalated()) {
            audit.record(ClaimAudit.line("ESCALATE", claim, by, name));
        }
    }
}
