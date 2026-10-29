package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
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

    /**
     * Whether the claim editor would save {@code claim} with only its amount changed to {@code amount} by {@code user}:
     * a draft whose editor no other user has open, not due by a date inside a closed period, not dated after its
     * person's retirement, and without lines adding up to another amount.
     */
    public static boolean editableAmount(Claim claim, BigDecimal amount, String user, Claims claims, ClaimLocks locks,
            Periods periods, People people) {
        if (claim == null || claim.status() != ClaimStatus.DRAFT) {
            return false;
        }
        String by = locks.lockedBy(claim);
        if (by != null && !by.equals(user.trim())) {
            return false;
        }
        if (periods.closedOn(claim.sheetId(), claim.dueBy()) != null) {
            return false;
        }
        try {
            RefuseRetiredInput.check(people.find(claim.sheetId(), claim.personId()), claim.date());
        } catch (ClaimAfterRetirementException e) {
            return false;
        }
        List<ClaimLine> lines = claims.lines(claim);
        return lines.isEmpty() || amount.compareTo(ClaimLines.total(lines)) == 0;
    }
}
