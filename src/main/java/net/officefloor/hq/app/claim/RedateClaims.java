package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Re-dates each selected claim that can be re-dated, in list order (people in list order, each
 *  person's newest claim first). Each is re-dated as a single re-dating would be — only by a
 *  steward, only an OPEN or PENDING claim, never one due in a closed period nor to a date after the
 *  person's retirement — with its own audit line, then the figures it moved. One that cannot (or is
 *  not on the sheet) is left as it is and counted as refused. */
public class RedateClaims {
    public void redate(@PathVariable("sheetId") String sheetId, @Val BulkRedate bulk, Claims claims, People people,
            Settings settings, Sheets sheets, ClosedPeriods closedPeriods, Rollup rollup, Audit audit,
            Out<BulkRedateResult> result) {
        ApplyClaimRedate apply = new ApplyClaimRedate();
        RecordClaimRedated record = new RecordClaimRedated();
        int redated = 0;
        List<Claim> all = claims.onSheet(sheetId);
        for (Person person : people.onSheet(sheetId)) {
            for (Claim listed : all) {
                if (listed.getPersonId() != person.getId() || !bulk.claimIds().contains(listed.getId())) continue;
                Claim claim = claims.find(sheetId, listed.getId());
                ClaimRedate change = new ClaimRedate(bulk.date(), bulk.user());
                Snapshot before = rollup.snapshot(sheetId);
                try {
                    apply.apply(change, claim, people, settings, sheets, closedPeriods);
                } catch (ClaimStatusRefusedException | PeriodClosedException | PersonRetiredException e) {
                    continue;
                }
                claims.save(claim);
                record.record(change, claim, people, audit);
                rollup.record(sheetId, before, bulk.user());
                redated++;
            }
        }
        result.set(new BulkRedateResult(redated, bulk.claimIds().size() - redated));
    }
}
