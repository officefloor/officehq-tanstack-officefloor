package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored move in the audit file, naming the claim like every claim line (with its new person), then
 *  the person it moved from and to. */
public class AuditClaimReassigned {
    public void audit(@Val ReassignInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        var from = people.find(loaded.sheetId(), loaded.personId());
        var to = people.find(loaded.sheetId(), saved.personId());
        audit.record("CLAIM_REASSIGNED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + to.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " from=" + from.name() + " to=" + to.name());
    }
}
