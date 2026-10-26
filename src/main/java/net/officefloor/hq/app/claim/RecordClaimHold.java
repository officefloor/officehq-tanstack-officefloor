package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_HELD or CLAIM_RELEASED, naming the claim. */
public class RecordClaimHold {
    public void record(@Val Claim claim, @Val ChangedBy by, PersonRepository people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line(claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED", claim, by,
                person == null ? "" : person.getName()));
    }
}
