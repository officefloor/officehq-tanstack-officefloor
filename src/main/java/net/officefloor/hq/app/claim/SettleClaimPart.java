package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Watches;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.hq.app.voidreason.VoidReasons;
import net.officefloor.plugin.variable.Val;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.bind.annotation.PathVariable;

/** Part-settles an OPEN claim: the amount is split off it exactly as the split button does (CLAIM_SPLIT,
 *  the new claim's CLAIM_OPENED, then the figures it moved), then the new claim is settled by the user
 *  exactly as the settle button does (CLAIM_SETTLED, or CLAIM_PENDING over the clerk limit, then the
 *  figures it moved). The four-eyes rules and the proof rule are judged on the original claim as it
 *  stood before the split (the new claim's opener is the user, who may still part-settle). A split or a
 *  settle that would be refused refuses the whole action: its own transaction is rolled back and the
 *  held audit lines are never written. */
public class SettleClaimPart {
    public void settle(@PathVariable("sheetId") String sheetId, @PathVariable("claimId") long claimId,
            @Val ClaimSplit split, Claims claims, ClaimNumbers numbers, People people, Figures figures,
            ClosedPeriods closedPeriods, Settings settings, Holidays holidays, ClaimLog log, Sheets sheets,
            VoidReasons voidReasons, Watches watches, Rollup rollup, Audit audit,
            PlatformTransactionManager transactionManager)
            throws ClaimNotFoundException, ClaimStatusRefusedException, PeriodClosedException {
        BufferedAudit held = new BufferedAudit(settings);
        // In a transaction of its own, rolled back when the split or the settle is refused: nothing is kept.
        Exception refused = new TransactionTemplate(transactionManager).execute(status -> {
            try {
                Claim judged = claims.find(sheetId, claimId);
                if (judged == null) throw new ClaimNotFoundException(claimId);
                Claim claim = claims.find(sheetId, claimId);
                Snapshot before = rollup.snapshot(sheetId);
                new ApplyClaimSplit().apply(split, claim, numbers, people, closedPeriods, settings, holidays);
                new SaveClaimSplit().save(split, claim, claims);
                new RecordClaimSplit().record(split, claim, people, log, held);
                rollup.record(sheetId, before, split.user(), held);

                Claim into = split.into();
                ClaimStatusChange settle = new ClaimStatusChange(ClaimAction.SETTLE, split.user());
                before = rollup.snapshot(sheetId);
                new ApplyClaimStatus().apply(settle, into, judged, numbers, people, figures, closedPeriods, settings,
                        log, sheets, voidReasons);
                claims.save(into);
                new RecordClaimStatusChanged().record(settle, into, people, log, watches, held);
                rollup.record(sheetId, before, split.user(), held);
                return null;
            } catch (ClaimNotFoundException | ClaimStatusRefusedException | PeriodClosedException e) {
                status.setRollbackOnly();
                return e;
            }
        });
        if (refused instanceof ClaimNotFoundException e) throw e;
        if (refused instanceof ClaimStatusRefusedException e) throw e;
        if (refused instanceof PeriodClosedException e) throw e;
        held.flush(audit);
    }
}
