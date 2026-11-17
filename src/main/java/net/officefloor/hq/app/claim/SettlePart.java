package net.officefloor.hq.app.claim;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.voidreason.VoidReasonRepository;
import net.officefloor.hq.app.watch.WatchRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Part-settles an OPEN claim: splits the amount off it through the very steps of the split (ApplySplit, SaveSplit),
 * then settles the new claim by the same user through the very steps of the settle (ApplyClaimStatus, SaveClaimStatus;
 * a clerk's settle above the clerk limit making it PENDING). The four-eyes rules and the proof rule are judged on the
 * original claim as it was before the split (the new claim's opener is the user who part-settles). All or nothing: a
 * split or a settle that would be refused refuses the whole, writing nothing. Once both are stored, writes the split's
 * lines (CLAIM_SPLIT, CLAIM_OPENED and the figures the split moved), then the settle's (see RecordClaimStatusChanged).
 */
public class SettlePart {
    public void settle(@Val Claim claim, @Val ClaimSplit split, @Val RollupBefore before, ClaimRepository claims,
            PersonRepository people, RateRepository rates, PeriodRepository periods, SettingRepository settings,
            RoleRepository roles, SheetRepository sheets, HolidayRepository holidays, WatchRepository watches,
            VoidReasonRepository voidReasons, ClaimLog log, Rollup rollup, Audit audit, Out<SplitOff> made)
            throws InvalidClaimStatusException {
        ClaimStatusChange change = new ClaimStatusChange(ClaimAction.SETTLE, ClaimStatus.SETTLED, split.by(), "");
        Claim[] remaining = new Claim[1];
        new ApplySplit().apply(claim, split, sheets, periods, settings, roles, (c) -> remaining[0] = c);
        ApplyClaimStatus.applyJudged(claim, claim, change, people, rates, periods, settings, roles, sheets, claims, log,
                voidReasons, (c) -> { }, (n) -> { }, (p) -> { });

        SplitOff[] off = new SplitOff[1];
        new SaveSplit().save(remaining[0], split, claims, people, periods, settings, holidays, sheets, log,
                (s) -> off[0] = s);
        List<String> splitLines = RecordClaimSplit.lines(remaining[0], off[0], split, people);
        List<String> splitRollup = rollup.lines(claim.sheetId(), split.by(), before);

        RollupBefore beforeSettle = rollup.take(claim.sheetId());
        Claim added = claims.find(claim.sheetId(), off[0].claim().id());
        Claim[] changed = new Claim[1];
        Claim[] stored = new Claim[1];
        ClaimNumber[] number = new ClaimNumber[1];
        PreviousDue[] previous = new PreviousDue[1];
        ApplyClaimStatus.applyJudged(added, claim, change, people, rates, periods, settings, roles, sheets, claims, log,
                voidReasons, (c) -> changed[0] = c, (n) -> number[0] = n, (p) -> previous[0] = p);
        new SaveClaimStatus().save(changed[0], change, claims, periods, settings, sheets, log, voidReasons,
                (c) -> stored[0] = c, (n) -> number[0] = n);

        splitLines.forEach(audit::record);
        splitRollup.forEach(audit::record);
        new RecordClaimStatusChanged().record(stored[0], change, number[0], previous[0], people, watches, beforeSettle,
                rollup, audit);
        made.set(new SplitOff(stored[0]));
    }
}
