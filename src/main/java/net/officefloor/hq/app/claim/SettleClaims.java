package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Settles the selected claims all or nothing. When every one can be settled, each is settled in list order — the
 * people in the order they were added, each one's claims newest first — exactly as settling it alone would (the same
 * audit and rollup lines; one a clerk's settle leaves PENDING counts as settled), the press is the sheet's next batch
 * and each claim carries its number, and a BATCH_SETTLED line follows with the count and the frozen figures of those
 * SETTLED added up. If any one would be refused (or is not on the sheet), none is settled, all count as refused and
 * nothing is written.
 */
public class SettleClaims {
    public void settle(@Val SheetResponse sheet, @Val BulkSettleIds selected, @Val ChangedBy by, ClaimRepository claims,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods, Rollup rollup, Audit audit, ClaimLog log, ClaimLogRepository logs, WatchRepository watches,
            VoidReasonRepository voidReasons,
            Out<BulkSettleResponse> result) {
        if (roles.isViewer(sheet.id(), by.name()) || sheets.isArchived(sheet.id())) {
            // A viewer's press, or one on an archived sheet, goes through with nothing done: every selected claim is
            // refused.
            result.set(new BulkSettleResponse(0, selected.ids().size()));
            return;
        }
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RefuseClosedClaim closed = new RefuseClosedClaim();
        RecordClaimStatus record = new RecordClaimStatus();
        int[] counts = new int[1];
        BigDecimal[] total = { BigDecimal.ZERO };
        Integer[] batch = new Integer[1];
        audit.hold();
        boolean kept = false;
        try {
            kept = claims.keepOnly(() -> {
                for (Person person : people.list(sheet.id())) {
                    for (Claim claim : claims.listOf(sheet.id(), person.getId())) {
                        if (!selected.ids().contains(claim.getId())) {
                            continue;
                        }
                        try {
                            closed.check(claim, periods);
                            RollupSnapshot before = rollup.snapshot(sheet.id());
                            ClaimNumber[] number = new ClaimNumber[1];
                            Out<ClaimNumber> audited = value -> number[0] = value;
                            apply.apply(ClaimStatus.SETTLED, claim, claims, people, rates, roles, settings, sheets, periods, logs, by, ClaimReview.NONE, VoidChoice.NONE, voidReasons, audited);
                            record.record(claim, number[0], by, ClaimReview.NONE, people, watches, logs, audit, log);
                            rollup.record(sheet.id(), before, by);
                        } catch (BadRequestException refused) {
                            // One refusal refuses them all: nothing is settled or written.
                            return false;
                        }
                        if (batch[0] == null) {
                            batch[0] = claims.nextSettleBatch(sheet.id());
                        }
                        claims.markSettleBatch(claim, batch[0]);
                        if (claim.getStatus() == ClaimStatus.SETTLED && claim.getSettledDue() != null) {
                            total[0] = total[0].add(claim.getSettledDue());
                        }
                        counts[0]++;
                    }
                }
                // A selected id not on this sheet's list cannot be settled either.
                return counts[0] == selected.ids().size();
            });
            if (kept) {
                audit.record("BATCH_SETTLED sheet=" + sheet.id() + " by=" + by.name() + " batch=" + batch[0]
                        + " count=" + counts[0] + " total=" + total[0].setScale(2, RoundingMode.HALF_UP).toPlainString());
            }
        } finally {
            audit.release(kept);
        }
        int settled = kept ? counts[0] : 0;
        result.set(new BulkSettleResponse(settled, selected.ids().size() - settled));
    }
}
