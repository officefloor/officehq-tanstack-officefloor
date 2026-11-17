package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes CLAIM_DUE_BY_SET to the audit file once the due-by is stored, naming the claim as every claim line does
 *  (number=<number> when it has one), ending old=<old due-by> new=<new due-by>. Then writes the figures the change
 *  moved (see Rollup). */
public class RecordClaimDueBySet {
    public void record(@Val Claim claim, @Val ClaimDueBySetting setting, @Val RollupBefore before, PersonRepository people,
            Rollup rollup, Audit audit) {
        Person person = people.find(claim.sheetId(), claim.personId());
        audit.record("CLAIM_DUE_BY_SET sheet=" + claim.sheetId() + " by=" + setting.by() + " person="
                + (person == null ? "" : person.name()) + " amount=" + claim.amount().toPlainString()
                + " date=" + claim.date() + (claim.number() == null ? "" : " number=" + claim.number()) + " old="
                + setting.old() + " new=" + setting.dueBy());
        rollup.record(claim.sheetId(), setting.by(), before);
    }
}
