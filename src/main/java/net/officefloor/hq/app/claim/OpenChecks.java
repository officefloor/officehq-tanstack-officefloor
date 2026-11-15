package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The refusals {@link OpenClaim} gives a claim, for steps outside this package that open claims as
 *  the open button would (a step class may have only one public method, so they are not public there). */
public final class OpenChecks {
    private OpenChecks() {
    }

    /** See {@link OpenClaim#requireNote}. */
    public static void requireNote(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) throws ClaimStatusException {
        OpenClaim.requireNote(claim, people, rates, settings, sheets);
    }

    /** See {@link OpenClaim#requireUnderCap}. */
    public static void requireUnderCap(Claim claim, ClaimRepository claims, PeriodRepository periods)
            throws ClaimStatusException {
        OpenClaim.requireUnderCap(claim, claims, periods);
    }
}
