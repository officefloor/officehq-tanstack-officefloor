package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored review assignment in the audit file, naming the claim like every claim line, then the user
 *  assigned. */
public class AuditReviewAssigned {
    public void audit(@Val ReviewAssignInput input, @Val Claim loaded, @Val ClaimResponse saved,
            PersonRepository people, Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("REVIEW_ASSIGNED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "") + " user=" + input.reviewer());
    }
}
