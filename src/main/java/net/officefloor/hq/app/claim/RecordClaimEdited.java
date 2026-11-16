package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_EDITED to the audit file once the edit is stored. Then writes the
 * figures the change moved (see Rollup). */
public class RecordClaimEdited {
    public void record(@Val Claim claim, @Val ClaimValues values, PersonRepository people, @Val RollupBefore before, Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_EDITED sheet=" + claim.sheetId() + " by=" + values.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date());
        rollup.record(claim.sheetId(), values.by(), before);
    }
}
