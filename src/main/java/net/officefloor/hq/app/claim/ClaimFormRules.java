package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.Person;

/** The claim form's rules on a typed body, for a feature outside claims that must take only what the form takes. */
public final class ClaimFormRules {

    private ClaimFormRules() {
    }

    public static ClaimInput parse(ClaimRequest body) throws InvalidClaimException {
        return ValidateClaim.parse(body);
    }

    /** Refuses a new claim due by {@code dueBy} when that falls inside a closed period, as the claim form does. */
    public static void refuseClosed(Periods periods, String sheet, LocalDate dueBy) throws ClaimInClosedPeriodException {
        RefuseClosedNewInput.check(periods, sheet, dueBy);
    }

    /** Refuses a claim dated {@code date} for a person retired on it, as the claim form does. */
    public static void refuseRetired(Person person, LocalDate date) throws ClaimAfterRetirementException {
        RefuseRetiredInput.check(person, date);
    }
}
