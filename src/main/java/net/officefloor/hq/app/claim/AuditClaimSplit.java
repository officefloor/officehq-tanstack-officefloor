package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored split in the audit file: CLAIM_SPLIT naming the original claim like every claim line, with the
 *  amount it kept, then the new claim's number; then CLAIM_OPENED for the new claim. */
public class AuditClaimSplit {
    public void audit(@Val SplitInput input, @Val Split split, PersonRepository people, Audit audit) {
        var original = ClaimResponse.from(split.original());
        var added = ClaimResponse.from(split.added());
        var person = people.find(split.original().sheetId(), split.original().personId());
        audit.record("CLAIM_SPLIT sheet=" + split.original().sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + original.amount() + " date=" + original.date()
                + (original.number() != null ? " number=" + original.number() : "")
                + " into=" + added.number());
        audit.record("CLAIM_OPENED sheet=" + split.added().sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + added.amount() + " date=" + added.date()
                + " number=" + added.number());
    }
}
