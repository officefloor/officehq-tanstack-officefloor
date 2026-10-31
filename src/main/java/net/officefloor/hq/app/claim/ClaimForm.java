package net.officefloor.hq.app.claim;

import net.officefloor.hq.app.holiday.Holidays;
import net.officefloor.hq.app.period.ClosedPeriods;
import net.officefloor.hq.app.period.PeriodClosedException;
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
}
