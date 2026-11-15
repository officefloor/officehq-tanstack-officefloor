package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.rate.RateRepository;
import net.officefloor.hq.app.setting.RoleRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The refusals {@link OpenClaim} gives a claim, for steps outside this package that open claims as
 *  the open button would (a step class may have only one public method, so they are not public there). */
public final class OpenChecks {
    private OpenChecks() {
    }

    /** See {@link OpenClaim#opensTo}. */
    public static ClaimStatus opensTo(Claim claim, PersonRepository people, RateRepository rates,
            SettingRepository settings, SheetRepository sheets) throws ClaimStatusException {
        return OpenClaim.opensTo(claim, people, rates, settings, sheets);
    }

    /** See {@link OpenClaim#requireFlagSteward}. */
    public static void requireFlagSteward(Claim claim, String by, PersonRepository people, RoleRepository roles)
            throws ClaimStatusException {
        OpenClaim.requireFlagSteward(claim, by, people, roles);
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

    /** See {@link OpenClaim#requireUnderMaxOpen}. */
    public static void requireUnderMaxOpen(Claim claim, ClaimRepository claims, SettingRepository settings)
            throws ClaimStatusException {
        OpenClaim.requireUnderMaxOpen(claim, claims, settings);
    }
}
