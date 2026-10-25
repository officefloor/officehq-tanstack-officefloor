package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_REDATED, naming the claim with its new date (and number), then its old and new dates. */
public class RecordClaimRedated {
    public void record(@Val Claim claim, @Val RedateTo to, @Val ChangedBy by, PersonRepository people, Audit audit) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        audit.record(ClaimAudit.line("CLAIM_REDATED", claim, by, person == null ? "" : person.getName())
                + " old=" + to.oldDate() + " new=" + to.date());
    }
}
