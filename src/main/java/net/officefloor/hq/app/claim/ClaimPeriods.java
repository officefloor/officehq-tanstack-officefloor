package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** A claim belongs to the period its due-by falls in: the due-by fixed when it was added, which an edit, a
 *  re-dating or a move to another person leaves as it is. */
public final class ClaimPeriods {
    private ClaimPeriods() {
    }

    /** The due-by a claim of the person dated {@code date} would be fixed with, were it added now: its date
     *  plus the terms it would get (see {@link SaveClaimAdded#termsOf}). */
    public static LocalDate dueByOf(Person person, LocalDate date, SettingStore settings) {
        return date.plusDays(SaveClaimAdded.termsOf(person, date, settings));
    }

    /** Refuses a claim due by {@code dueBy} when that falls in a closed period. */
    public static void refuseClosed(PeriodStore periods, String sheetId, LocalDate dueBy) throws InvalidSheetException {
        PeriodResponse closed = periods.closedOn(sheetId, dueBy);
        if (closed != null) {
            throw new InvalidSheetException("The period " + closed.name() + " is closed");
        }
    }
}
