package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored share: CLAIM_SHARED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>
 *  with=<holder's name> share=<0.00>; then how it moved the sheet's figures. */
public class AuditClaimShared {
    public void audit(@Val ShareInput input, @Val Claim claim, @Val Person holder, @Val Rollup before, PersonStore people,
            Audit audit, RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_SHARED sheet=" + claim.getSheetId() + " by=" + input.user() + " person=" + person.getName()
                + " amount=" + shown.amount() + " date=" + shown.date() + " with=" + holder.getName() + " share="
                + input.share().setScale(2).toPlainString());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
