package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Settles each selected claim that can be settled, in list order (people in list order, each
 *  person's newest claim first), by the signed-in user. Each is settled as a single settle would
 *  be — its own audit line, then the figures it moved; one that goes PENDING counts as settled. One
 *  that cannot be settled (or is not on the sheet) is left as it is and counted as refused. */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettle bulk, Claims claims, People people,
            ClaimNumbers numbers, Figures figures, ClosedPeriods closedPeriods, Settings settings, Rollup rollup,
            ClaimLog log, Watches watches, Audit audit, Out<BulkSettleResult> result) {
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.SETTLE, bulk.user());
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RecordClaimStatusChanged record = new RecordClaimStatusChanged();
        List<Claim> all = claims.onSheet(sheetId);
        int settled = 0;
        for (Person person : people.onSheet(sheetId)) {
            for (Claim listed : all) {
                if (listed.getPersonId() != person.getId() || !bulk.claimIds().contains(listed.getId())) continue;
                Claim claim = claims.find(sheetId, listed.getId());
                Snapshot before = rollup.snapshot(sheetId);
                try {
                    apply.apply(change, claim, numbers, people, figures, closedPeriods, settings);
                } catch (ClaimStatusRefusedException | PeriodClosedException e) {
                    continue;
                }
                claims.save(claim);
                record.record(change, claim, people, log, watches, audit);
                rollup.record(sheetId, before, bulk.user());
                settled++;
            }
        }
        result.set(new BulkSettleResult(settled, bulk.claimIds().size() - settled));
    }
}
