package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored edit in the audit file, with the amount and date as the screen shows them,
 *  and the claim's number when it has one. */
public class AuditClaimEdited {
    public void audit(@Val ClaimInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        record(input.user(), loaded, people.find(loaded.sheetId(), loaded.personId()), saved, audit);
    }

    static void record(String user, Claim loaded, Person person, ClaimResponse saved, Audit audit) {
        audit.record("CLAIM_EDITED sheet=" + loaded.sheetId() + " by=" + user
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : ""));
    }
}
