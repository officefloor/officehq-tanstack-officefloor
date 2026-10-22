package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored edit in the audit file, with the amount and date as the screen shows them,
 *  and the claim's number when it has one. */
public class AuditClaimEdited {
    public void audit(@Val ClaimInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("CLAIM_EDITED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : ""));
    }
}
