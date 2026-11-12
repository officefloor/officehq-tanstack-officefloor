package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored re-dating: CLAIM_REDATED sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<new yyyy-mm-dd> number=<n> old=<yyyy-mm-dd> new=<yyyy-mm-dd>. */
public class AuditClaimRedated {
    public void audit(@Val RedateInput input, @Val Claim claim, @Val Redating redating, @Val Rollup before,
            PersonStore people, Audit audit, RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_REDATED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber())
                + " old=" + redating.old() + " new=" + redating.now());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
