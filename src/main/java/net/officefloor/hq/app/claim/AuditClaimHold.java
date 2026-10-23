package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored hold (CLAIM_HELD) or release (CLAIM_RELEASED) in the audit file, naming the claim like every
 *  claim line. */
public class AuditClaimHold {
    public void audit(@Val ClaimHoldInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record((saved.held() ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + loaded.sheetId() + " by="
                + input.user() + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : ""));
    }
}
