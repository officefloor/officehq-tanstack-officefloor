package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayStore;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.InvalidSheetException;

/** A claim belongs to the period its due-by falls in: the due-by fixed when it was added, which an edit, a
 *  re-dating or a move to another person leaves as it is (only a holiday added later re-fixes it). */
public final class ClaimPeriods {
    private ClaimPeriods() {
    }

    /** The due-by a claim of the person dated {@code date} would be fixed with, were it added now: its date
     *  the terms-th business day after it, for the terms in force on the sheet's today it would get (see {@link SaveClaimAdded#termsOf}),
     *  skipping weekends and the holidays on the sheet now. */
    public static LocalDate dueByOf(Person person, LocalDate date, SettingStore settings, HolidayStore holidays) {
        return holidays.businessDaysAfter(person.getSheetId(), date, SaveClaimAdded.termsOf(person, settings));
    }

    /** Refuses opening the claim when the period it belongs to caps its open claims and the claim's person
     *  already has that many OPEN or PENDING claims in it. */
    public static void refuseCapped(PeriodStore periods, ClaimStore claims, Claim claim) throws InvalidSheetException {
        if (claim.getDueBy() == null) {
            return;
        }
        PeriodResponse period = periods.on(claim.getSheetId(), claim.getDueBy());
        if (period == null || period.cap() == null) {
            return;
        }
        int open = claims.countOpenDue(claim.getSheetId(), claim.getPersonId(), LocalDate.parse(period.from()),
                LocalDate.parse(period.to()));
        if (open >= period.cap()) {
            throw new InvalidSheetException("The period " + period.name() + " allows " + period.cap()
                    + " open claims per person");
        }
    }

    /** Refuses opening the claim when the sheet has a maximum of open claims per person and the claim's person
     *  already has that many OPEN or PENDING claims. */
    public static void refuseMaxOpen(SettingStore settings, ClaimStore claims, Claim claim) throws InvalidSheetException {
        Integer max = settings.maxOpen(claim.getSheetId());
        if (max != null && claims.countOpen(claim.getSheetId(), claim.getPersonId()) >= max) {
            throw new InvalidSheetException("A person may have at most " + max + " open claims");
        }
    }

    /** Refuses a claim due by {@code dueBy} when that falls in a closed period. */
    public static void refuseClosed(PeriodStore periods, String sheetId, LocalDate dueBy) throws InvalidSheetException {
        PeriodResponse closed = periods.closedOn(sheetId, dueBy);
        if (closed != null) {
            throw new InvalidSheetException("The period " + closed.name() + " is closed");
        }
    }
}
