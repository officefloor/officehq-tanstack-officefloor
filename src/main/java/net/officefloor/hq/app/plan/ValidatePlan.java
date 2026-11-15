package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.claim.ClaimForm;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.hq.app.claim.ValidClaim;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for an amount the claim form would refuse (a plan makes no corrections, so not negative), a
 *  start or end that is not a date, an interval that is not a whole number of at least 1, and an end
 *  before the start. */
public class ValidatePlan {
    public void validate(@RequestBody PlanRequest request, Out<ValidPlan> body) throws InvalidPlanException {
        if (request == null) throw new InvalidPlanException("A plan is required");
        ValidClaim amount;
        try {
            amount = ClaimForm.check(new ClaimRequest(request.amount(), "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        if (amount.isCorrection()) throw new InvalidPlanException("A plan's amount is not negative");
        LocalDate start = date(request.start());
        LocalDate end = date(request.end());
        String every = request.every() == null ? "" : request.every().trim();
        if (!every.matches("\\d{1,9}") || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("Every N days needs a whole number of at least 1");
        }
        if (end.isBefore(start)) throw new InvalidPlanException("The end is before the start");
        body.set(new ValidPlan(amount.amount(), start, Integer.parseInt(every), end));
    }

    private static LocalDate date(String text) throws InvalidPlanException {
        String t = text == null ? "" : text.trim();
        try {
            return LocalDate.parse(t);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("Not a date: " + t);
        }
    }
}
