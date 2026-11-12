package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records an added line: CLAIM_LINE_ADDED sheet=<id> by=<user> person=<name> amount=<new 0.00> date=<yyyy-mm-dd>
 *  label=<label> qty=<qty> unit=<0.00>. */
public class AuditClaimLineAdded {
    public void audit(@Val LineInput input, @Val Claim claim, @Val Rollup before, PersonStore people, Audit audit,
            RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_LINE_ADDED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date() + " label=" + input.label()
                + " qty=" + input.qty() + " unit=" + input.unit().toPlainString());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
