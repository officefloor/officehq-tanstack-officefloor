package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored share in the audit file, naming the claim like every claim line, then the person it is shared
 *  with and their share. */
public class AuditClaimShared {
    public void audit(@Val ShareInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        var with = people.find(loaded.sheetId(), input.personId());
        audit.record("CLAIM_SHARED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " with=" + with.name() + " share=" + input.share().toPlainString());
    }
}
