package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.plugin.variable.Val;

/** Record the stored line in the audit file, naming the claim like every claim line with its new amount, then the
 *  line's label, quantity and unit amount. */
public class AuditLineAdded {
    public void audit(@Val LineInput input, @Val Claim loaded, @Val ClaimResponse saved, PersonRepository people,
            Audit audit) {
        var person = people.find(loaded.sheetId(), loaded.personId());
        audit.record("CLAIM_LINE_ADDED sheet=" + loaded.sheetId() + " by=" + input.user()
                + " person=" + person.name() + " amount=" + saved.amount() + " date=" + saved.date()
                + (saved.number() != null ? " number=" + saved.number() : "")
                + " label=" + input.label() + " qty=" + input.qty() + " unit=" + input.unit().toPlainString());
    }
}
