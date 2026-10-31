package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Opens each selected claim that can be opened, in list order (people in list order, each
 *  person's newest claim first), so their numbers follow that order. Each is opened as a single
 *  open would be — its own audit line, then the figures it moved; one that cannot be opened (or is
 *  not on the sheet) is left as it is and counted as refused. */
public class OpenClaims {
    public void open(@PathVariable("sheetId") String sheetId, @Val BulkOpen bulk, Claims claims, People people,
            ClaimNumbers numbers, Figures figures, ClosedPeriods closedPeriods, Settings settings, Rollup rollup,
            Audit audit, Out<BulkOpenResult> result) {
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.OPEN, bulk.user());
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RecordClaimStatusChanged record = new RecordClaimStatusChanged();
        List<Claim> all = claims.onSheet(sheetId);
        int opened = 0;
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
                record.record(change, claim, people, audit);
                rollup.record(sheetId, before, bulk.user());
                opened++;
            }
        }
        result.set(new BulkOpenResult(opened, bulk.claimIds().size() - opened));
    }
}
