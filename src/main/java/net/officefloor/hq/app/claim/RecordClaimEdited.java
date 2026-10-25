package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

public class RecordClaimEdited {
    public void record(@Val Claim claim, @Val ChangedBy by, PersonRepository people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_EDITED", claim, by, person == null ? "" : person.getName()));
    }
}
