package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored due-by in the audit file, naming the claim like every claim line, then its old and new due-by. */
public class AuditClaimDueBySet {
    public void audit(@Val DueBySetInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("CLAIM_DUE_BY_SET sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " old=" + loaded.dueBy() + " new=" + input.dueBy());
    }
}
