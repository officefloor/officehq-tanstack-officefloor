package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored status change: CLAIM_OPENED, CLAIM_PENDING, CLAIM_SETTLED, CLAIM_APPROVED,
 *  CLAIM_REJECTED or CLAIM_VOIDED sheet=<id> by=<user> person=<name> amount=<0.00> date=<yyyy-mm-dd>,
 *  then number=<n> when the claim has or had a number, then for CLAIM_SETTLED and CLAIM_APPROVED
 *  due=<0.00> as fixed on settling (n/a when its grade had no rate), for CLAIM_REJECTED reason=<reason>. */
public class AuditClaimStatus {
    public void audit(@Val StatusInput input, @Val StatusMove move, @Val Claim claim, @Val StatusChange change, @Val Rollup before,
            PersonStore people, Audit audit, RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record(move.to().event(move.from()) + " sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (change.number() == null ? "" : " number=" + change.number())
                + (move.to() == ClaimStatus.SETTLED ? " due=" + (shown.due() == null ? "n/a" : shown.due()) : "")
                + (move.from() == ClaimStatus.PENDING && move.to() == ClaimStatus.OPEN ? " reason=" + input.reason() : ""));
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
