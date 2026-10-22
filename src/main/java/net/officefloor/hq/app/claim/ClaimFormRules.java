package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The claim form's rules (see ValidateClaim) for steps outside this package that add a claim, or keep values a claim
 *  will be added with, by them. Not a step itself. */
public final class ClaimFormRules {
    private ClaimFormRules() {
    }

    /** The claim form's add, checked as ValidateClaim checks it. */
    public static ClaimInput check(ClaimRequest request) throws InvalidClaimException {
        return ValidateClaim.check(request);
    }

    /** Store one checked claim as the claim form does, and write its CLAIM_ADDED line (the caller records how the
     *  sheet's figures moved). */
    public static Claim add(ClaimInput input, Person person, SheetRepository sheets, SettingRepository settings,
            HolidayRepository holidays, PeriodRepository periods, ClaimRepository repository, ClaimLogRepository log,
            Audit audit) {
        Claim added = SaveNewClaim.insert(input, person, sheets, settings, holidays, periods, repository, log);
        AuditClaimAdded.record(input, person, added, audit);
        return added;
    }
}
