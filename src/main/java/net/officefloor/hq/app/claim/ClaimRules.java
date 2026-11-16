package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The rules a claim must keep to be added to a person (see AddClaim), checked without writing anything. */
public final class ClaimRules {

    private ClaimRules() {
    }

    /** The claim's date (the sheet's entry date, else its today, when none was given) and the due-by it would be fixed with (see dueBy);
     *  a due-by inside a closed period or a date after the person's retirement date is refused, as is a negative amount that does not
     *  correct a SETTLED claim (see Corrections). */
    public static Checked check(Person person, ClaimValues values, SettingRepository settings, PeriodRepository periods,
            ClaimRepository claims) throws InvalidClaimException {
        LocalDate date = values.date() != null ? values.date() : settings.claimDate(person.sheetId());
        LocalDate dueBy = dueBy(person, date, settings);
        if (periods.closedOn(person.sheetId(), dueBy)) {
            throw new InvalidClaimException("The claim's due-by is inside a closed period");
        }
        if (!person.mayClaimOn(date)) {
            throw new InvalidClaimException("The claim's date is after the person's retirement date");
        }
        return new Checked(date, Corrections.check(person.sheetId(), person.id(), values, claims), dueBy);
    }

    /** The due-by a claim of the person dated the date is fixed with as it is added: its date plus the person's own terms, else the
     *  terms of their grade in force on that date. */
    public static LocalDate dueBy(Person person, LocalDate date, SettingRepository settings) {
        return date.plusDays(settings.terms(person.sheetId(), person.terms(), person.gradeOn(date), date));
    }

    /** A claim's date, the number it corrects (null when none) and its due-by, once its rules are kept. */
    public record Checked(LocalDate date, String corrects, LocalDate dueBy) {
    }
}
