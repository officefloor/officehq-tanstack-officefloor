package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.access.LateCharge;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.WatchRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Part-settles an OPEN claim: splits the amount off exactly as a split does, then settles the new claim at once by the
 * user exactly as a settle does (PENDING when the clerk limit says so), with the same audit and rollup lines as the two
 * actions. The four-eyes rules and the proof rule are judged on the original claim as it was before the split (the new
 * claim's opener is the user). Either refused refuses the whole action: nothing is written.
 */
public class ApplySettlePart {
    public void apply(@Val Claim claim, @Val SplitAmount amount, @Val ChangedBy by, ClaimRepository repository,
            PersonRepository people, RateRepository rates, RoleRepository roles, SettingRepository settings,
            SheetRepository sheets, PeriodRepository periods, ClaimLogRepository logs, VoidReasonRepository voidReasons,
            WatchRepository watches, Rollup rollup, Audit audit, ClaimLog log, Out<Split> split)
            throws BadRequestException {
        String sheetId = claim.getSheetId();
        ApplyClaimStatus.refuseFourEyes(claim, by);
        LateCharge late = settings.lateCharge(sheetId, sheets.today(sheetId));
        BigDecimal due = ApplyClaimStatus.settleDue(claim, people, rates, settings, late);
        if (due != null) {
            ApplyClaimStatus.refuseWithoutProof(claim, due, people, settings, late);
        }
        BadRequestException[] refused = new BadRequestException[1];
        Split[] made = new Split[1];
        audit.hold();
        boolean kept = false;
        try {
            kept = repository.keepOnly(() -> {
                try {
                    RollupSnapshot before = rollup.snapshot(sheetId);
                    new ApplySplit().apply(claim, amount, by, repository, people, settings, sheets, periods,
                            value -> made[0] = value);
                    new RecordClaimSplit().record(claim, made[0], by, people, audit, log);
                    rollup.record(sheetId, before, by);
                    Claim into = made[0].into();
                    RollupSnapshot settling = rollup.snapshot(sheetId);
                    ClaimNumber[] number = new ClaimNumber[1];
                    new ApplyClaimStatus().change(ClaimStatus.SETTLED, into, repository, people, rates, roles, settings,
                            sheets, periods, logs, by, ClaimReview.NONE, VoidChoice.NONE, voidReasons,
                            value -> number[0] = value, true);
                    new RecordClaimStatus().record(into, number[0], by, ClaimReview.NONE, people, watches, logs, audit, log);
                    rollup.record(sheetId, settling, by);
                    return true;
                } catch (BadRequestException e) {
                    refused[0] = e;
                    return false;
                }
            });
        } finally {
            audit.release(kept);
        }
        if (!kept) {
            throw refused[0];
        }
        split.set(made[0]);
    }
}
