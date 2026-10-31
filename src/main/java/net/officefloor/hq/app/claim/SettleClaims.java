package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
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
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Settles the selected claims, all or nothing, by the signed-in user. If any of them cannot be
 *  settled (any refusal a single settle would give, or not on the sheet; going PENDING is not a
 *  refusal) none is settled, every one counts as refused and nothing is written. Otherwise each is
 *  settled in list order (people in list order, each person's newest claim first) as a single settle
 *  would be — its own audit line, then the figures it moved — and carries the sheet's next batch
 *  number; after the claims' own lines a BATCH_SETTLED line gives the batch, its count and the
 *  frozen dues of those SETTLED added up. */
public class SettleClaims {
    public void settle(@PathVariable("sheetId") String sheetId, @Val BulkSettle bulk, Claims claims, People people,
            ClaimNumbers numbers, Figures figures, ClosedPeriods closedPeriods, Settings settings, Rollup rollup,
            ClaimLog log, Sheets sheets, Watches watches, Audit audit, VoidReasons voidReasons, Out<BulkSettleResult> result) {
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.SETTLE, bulk.user());
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RecordClaimStatusChanged record = new RecordClaimStatusChanged();
        List<Claim> all = claims.onSheet(sheetId);
        List<Long> ordered = new ArrayList<>();
        for (Person person : people.onSheet(sheetId)) {
            for (Claim listed : all) {
                if (listed.getPersonId() == person.getId() && bulk.claimIds().contains(listed.getId())) ordered.add(listed.getId());
            }
        }
        int selected = bulk.claimIds().size();
        if (ordered.size() != selected) {
            result.set(new BulkSettleResult(0, selected));
            return;
        }
        // Check every claim first, on a copy that is never saved: one refusal refuses them all.
        for (Long id : ordered) {
            try {
                apply.apply(change, claims.find(sheetId, id), numbers, people, figures, closedPeriods, settings, log, sheets, voidReasons);
            } catch (ClaimStatusRefusedException | PeriodClosedException e) {
                result.set(new BulkSettleResult(0, selected));
                return;
            }
        }
        int batch = claims.nextBatch(sheetId);
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (Long id : ordered) {
            Claim claim = claims.find(sheetId, id);
            Snapshot before = rollup.snapshot(sheetId);
            try {
                apply.apply(change, claim, numbers, people, figures, closedPeriods, settings, log, sheets, voidReasons);
            } catch (ClaimStatusRefusedException | PeriodClosedException e) {
                throw new IllegalStateException("A checked claim was refused on settling", e);
            }
            claim.setBatch(batch);
            claims.save(claim);
            record.record(change, claim, people, log, watches, audit);
            rollup.record(sheetId, before, bulk.user());
            if (claim.getStatus() == ClaimStatus.SETTLED && claim.getDue() != null) total = total.add(claim.getDue());
        }
        audit.record("BATCH_SETTLED sheet=" + sheetId + " by=" + bulk.user() + " batch=" + batch
                + " count=" + ordered.size() + " total=" + total.setScale(2, RoundingMode.HALF_UP).toPlainString());
        result.set(new BulkSettleResult(selected, 0));
    }
}
