package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.Retirement;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** The claim form's add, step by step, for a change that adds several claims at once (a paste). */
public final class ClaimAdding {
    private ClaimAdding() {
    }

    /** The claim form's rules on a claim typed for the person, before anything is stored: the claim as
     *  it would be stored. */
    public static ClaimInput check(String sheetId, Person person, ClaimRequest request, SettingStore settings,
            PeriodStore periods, ClaimStore claims) throws InvalidSheetException {
        ClaimInput input = ValidateClaim.check(sheetId, request, settings);
        ClaimPeriods.refuseClosed(periods, sheetId, ClaimPeriods.dueByOf(person, input.date(), settings));
        Retirement.refuseClaimDate(person, input.date());
        CheckCorrection.refuseUnsettled(claims, sheetId, person.getId(), input);
        return input;
    }

    /** Stores the checked claim as a DRAFT and records it as CLAIM_ADDED (and LATE_ENTRY for a late entry), then how it moved the figures. */
    public static Claim add(ClaimInput input, Person person, ClaimStore store, SettingStore settings,
            ClaimLogStore log, PeriodStore periods, RollupService rollups, Audit audit) {
        var before = rollups.snapshot(person.getSheetId());
        Claim claim = SaveClaimAdded.add(input, person, store, settings, log, periods);
        AuditClaimAdded.record(input, person, claim, before, audit, rollups);
        return claim;
    }
}
