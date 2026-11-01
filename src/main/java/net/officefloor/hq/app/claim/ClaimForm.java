package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRetiredException;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;

/** The claim form's rules for work outside the claim package (not a pipeline step). */
public final class ClaimForm {
    private ClaimForm() {
    }

    /** The new DRAFT claim for these values and person, or refused by the claim form's rules. */
    public static Claim built(ClaimValues values, Person person, Sheets sheets, ClosedPeriods closedPeriods,
            Claims claims, Settings settings, Holidays holidays) throws PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        return BuildClaim.built(values, person, sheets, closedPeriods, claims, settings, holidays);
    }

    /** Edits the saved claim to these values and audits it as the claim form's edit does, or refused
     *  by the claim form's rules (then nothing is changed). */
    public static void edited(ClaimValues values, Claim claim, Sheets sheets, ClosedPeriods closedPeriods,
            People people, Claims claims, ClaimLines lines, Audit audit)
            throws ClaimEditRefusedException, ClaimLockedException, PeriodClosedException, PersonRetiredException, InvalidSheetRequestException {
        new ApplyClaim().apply(values, claim, sheets, closedPeriods, people, claims, lines);
        claims.save(claim);
        new RecordClaimEdited().record(values, claim, people, audit);
    }
}
