package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored hold toggle: CLAIM_HELD (or CLAIM_RELEASED) sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<yyyy-mm-dd>, then number=<n> when the claim has one; then how it moved the sheet's figures. */
public class AuditClaimHeld {
    public void audit(@Val HoldClaimInput input, @Val Claim claim, @Val Rollup before, PersonStore people, Audit audit,
            RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record((claim.isHeld() ? "CLAIM_HELD" : "CLAIM_RELEASED") + " sheet=" + claim.getSheetId() + " by="
                + input.user() + " person=" + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber()));
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
