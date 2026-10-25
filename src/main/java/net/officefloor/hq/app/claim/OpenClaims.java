package net.officefloor.hq.app.claim;

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
 * Opens each selected claim in list order — the people in the order they were added, each one's claims newest
 * first — so they are numbered in that order. Each is opened exactly as opening it alone would (the same refusals,
 * the same audit and rollup lines); one that cannot be opened is left as it is and counted as refused.
 */
public class OpenClaims {
    public void open(@Val SheetResponse sheet, @Val BulkOpenIds selected, @Val ChangedBy by, ClaimRepository claims,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods, Rollup rollup, Audit audit, ClaimLog log, ClaimLogRepository logs,
            WatchRepository watches, VoidReasonRepository voidReasons, Out<BulkOpenResponse> result) {
        int opened = 0;
        if (roles.isViewer(sheet.id(), by.name()) || sheets.isArchived(sheet.id())) {
            // A viewer's press, or one on an archived sheet, goes through with nothing done: every selected claim is
            // refused.
            result.set(new BulkOpenResponse(0, selected.ids().size()));
            return;
        }
        ApplyClaimStatus apply = new ApplyClaimStatus();
        RefuseClosedClaim closed = new RefuseClosedClaim();
        RecordClaimStatus record = new RecordClaimStatus();
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
                    apply.apply(ClaimStatus.OPEN, claim, claims, people, rates, roles, settings, sheets, periods, logs, by, ClaimReview.NONE, VoidChoice.NONE, voidReasons, audited);
                    record.record(claim, number[0], by, ClaimReview.NONE, people, watches, logs, audit, log);
                    rollup.record(sheet.id(), before, by);
                    opened++;
                } catch (BadRequestException refused) {
                    // left as it is
                }
            }
        }
        // A selected id not on this sheet's list cannot be opened either.
        result.set(new BulkOpenResponse(opened, selected.ids().size() - opened));
    }
}
