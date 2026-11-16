package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.PeriodRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.SettingRepository;

/** The rules a claim must keep to be added to a person (see AddClaim), checked without writing anything. */
public final class ClaimRules {

    private ClaimRules() {
    }

    /** The claim's date (the sheet's entry date, else its today, when none was given); a date inside a closed period or after the person's
     *  retirement date is refused, as is a negative amount that does not correct a SETTLED claim (see Corrections). */
    public static Checked check(Person person, ClaimValues values, SettingRepository settings, PeriodRepository periods,
            ClaimRepository claims) throws InvalidClaimException {
        LocalDate date = values.date() != null ? values.date() : settings.claimDate(person.sheetId());
        if (periods.closedOn(person.sheetId(), date)) {
            throw new InvalidClaimException("The claim's date is inside a closed period");
        }
        if (!person.mayClaimOn(date)) {
            throw new InvalidClaimException("The claim's date is after the person's retirement date");
        }
        return new Checked(date, Corrections.check(person.sheetId(), person.id(), values, claims));
    }

    /** A claim's date and the number it corrects (null when none), once its rules are kept. */
    public record Checked(LocalDate date, String corrects) {
    }
}
