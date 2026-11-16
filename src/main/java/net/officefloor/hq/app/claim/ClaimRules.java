package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.holiday.HolidayRepository;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The rules a claim must keep to be added to a person (see AddClaim), checked without writing anything. */
public final class ClaimRules {

    private ClaimRules() {
    }

    /** The claim's date (the sheet's entry date, else its today, when none was given) and the due-by it would be fixed with (see dueBy);
     *  a due-by inside a closed period or a date after the person's retirement date is refused, as is a negative amount that does not
     *  correct a SETTLED claim (see Corrections), or a reference already used on the sheet (see checkRef). */
    public static Checked check(Person person, ClaimValues values, SettingRepository settings, PeriodRepository periods,
            ClaimRepository claims, HolidayRepository holidays) throws InvalidClaimException {
        LocalDate date = values.date() != null ? values.date() : settings.claimDate(person.sheetId());
        int terms = terms(person, date, settings);
        LocalDate dueBy = holidays.businessDaysAfter(person.sheetId(), date, terms);
        if (periods.closedOn(person.sheetId(), dueBy)) {
            throw new InvalidClaimException("The claim's due-by is inside a closed period");
        }
        if (!person.mayClaimOn(date)) {
            throw new InvalidClaimException("The claim's date is after the person's retirement date");
        }
        checkRef(person.sheetId(), values, null, claims);
        return new Checked(date, Corrections.check(person.sheetId(), person.id(), values, claims), dueBy, terms);
    }

    /** Refuses a reference another claim of the sheet already has, comparing without regard to case; {@code self} is the
     *  claim being edited (null for an add), whose own reference does not count. */
    public static void checkRef(String sheetId, ClaimValues values, Long self, ClaimRepository claims) throws InvalidClaimException {
        if (values.ref() != null && claims.refUsed(sheetId, values.ref(), self)) {
            throw new InvalidClaimException("The reference " + values.ref() + " is already used on the sheet");
        }
    }

    /** The due-by a claim of the person dated the date is fixed with as it is added: the terms-th business day after its date (see
     *  HolidayRepository.businessDaysAfter), the terms being the person's own, else those of their grade in force on that date (the
     *  holidays on the sheet now decide; one added later works it out again, see ClaimRepository#refixDueBys). */
    public static LocalDate dueBy(Person person, LocalDate date, SettingRepository settings, HolidayRepository holidays) {
        return holidays.businessDaysAfter(person.sheetId(), date, terms(person, date, settings));
    }

    /** The terms a claim of the person dated the date is fixed with: the person's own, else those of their grade in force on
     *  that date. The claim keeps them, so its due-by can be worked out again when a holiday is added (see ClaimRepository#refixDueBys). */
    public static int terms(Person person, LocalDate date, SettingRepository settings) {
        return settings.terms(person.sheetId(), person.terms(), person.gradeOn(date), date);
    }

    /** Whether opening the claim would take its person past the cap of the period its due-by falls in: they already have
     *  that many OPEN or PENDING claims in it (a period with no cap, or no period: never). */
    public static boolean overCap(Claim claim, PeriodRepository periods, ClaimRepository claims) {
        Period period = periods.containing(claim.sheetId(), claim.dueBy());
        return period != null && period.cap() != null
                && claims.heldIn(claim.sheetId(), claim.personId(), period.from(), period.to()) >= period.cap();
    }

    /** A claim's date, the number it corrects (null when none), its due-by and the terms it was fixed with, once its rules are kept. */
    public record Checked(LocalDate date, String corrects, LocalDate dueBy, int terms) {
    }
}
