package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Val;

/** CLAIM_SPLIT, naming the claim with the amount it kept, then the new claim's number; then CLAIM_OPENED for the new claim and its OPEN status log line. */
public class RecordClaimSplit {
    public void record(@Val Claim claim, @Val Split split, @Val ChangedBy by, PersonRepository people, Audit audit,
            ClaimLog log) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String name = person == null ? "" : person.getName();
        audit.record(ClaimAudit.line("CLAIM_SPLIT", claim, by, name) + " into=" + split.into().getNumber());
        audit.record(ClaimAudit.line("CLAIM_OPENED", split.into(), by, name));
        log.record(split.into(), by);
    }
}
