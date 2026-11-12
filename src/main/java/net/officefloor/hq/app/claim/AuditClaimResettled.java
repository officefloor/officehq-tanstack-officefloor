package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.watch.Notify;
import net.officefloor.hq.app.watch.WatchStore;
import net.officefloor.plugin.variable.Val;

/** Records a stored re-settle: CLAIM_RESETTLED sheet=<id> by=<user> person=<name> amount=<0.00>
 *  date=<yyyy-mm-dd> number=<n> old=<0.00> new=<0.00> (n/a when there was no rate),
 *  then tells the person's watchers (NOTIFY), before the figures moved. */
public class AuditClaimResettled {
    public void audit(@Val ResettleInput input, @Val Claim claim, @Val Resettlement resettlement, @Val Rollup before,
            PersonStore people, Audit audit, RollupService rollups, WatchStore watches) {
        Person person = people.find(claim.getSheetId(), claim.getPersonId());
        ClaimResponse shown = ClaimResponse.from(claim);
        audit.record("CLAIM_RESETTLED sheet=" + claim.getSheetId() + " by=" + input.user() + " person="
                + person.getName() + " amount=" + shown.amount() + " date=" + shown.date()
                + (claim.getNumber() == null ? "" : " number=" + claim.getNumber())
                + " old=" + figure(resettlement.old()) + " new=" + figure(resettlement.now()));
        Notify.watchers(watches, audit, person, input.user(), claim.getNumber());
        rollups.record(claim.getSheetId(), input.user(), before);
    }

    private static String figure(BigDecimal due) {
        return due == null ? "n/a" : due.setScale(2).toPlainString();
    }
}
