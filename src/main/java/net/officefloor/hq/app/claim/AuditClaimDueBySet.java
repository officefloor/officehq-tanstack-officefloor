package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored due-by setting: CLAIM_DUE_BY_SET sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<yyyy-mm-dd> number=<n> (when it has one) old=<yyyy-mm-dd> new=<yyyy-mm-dd>, then how the figures moved. */
public class AuditClaimDueBySet {
    public void audit(@Val DueByInput input, @Val Claim claim, @Val DueBySetting setting, @Val Rollup before,
            PersonStore people, Audit audit, RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_DUE_BY_SET sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber())
                + " old=" + setting.old() + " new=" + setting.now());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
