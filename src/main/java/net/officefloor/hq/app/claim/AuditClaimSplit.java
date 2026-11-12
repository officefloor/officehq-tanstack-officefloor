package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.plugin.variable.Val;

/** Records a stored split: CLAIM_SPLIT sheet=<id> by=<user> person=<name> amount=<remaining 0.00>
 *  date=<yyyy-mm-dd> number=<n> into=<new number>, then CLAIM_OPENED for the new claim as an opening
 *  records it, then how the split moved the figures. */
public class AuditClaimSplit {
    public void audit(@Val SplitInput input, @Val Claim claim, @Val Split split, @Val Rollup before, PersonStore people,
            Audit audit, RollupService rollups) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        String who = " sheet=" + claim.getSheetId() + " by=" + input.user() + " person=" + person.getName();
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_SPLIT" + who + " amount=" + shown.amount() + " date=" + shown.date()
                + " number=" + claim.getNumber() + " into=" + split.into().getNumber());
        ClaimResponse into = ClaimResponse.from(split.into());
        audit.record(ClaimStatus.OPEN.event() + who + " amount=" + into.amount() + " date=" + into.date()
                + " number=" + split.into().getNumber());
        rollups.record(claim.getSheetId(), input.user(), before);
    }
}
