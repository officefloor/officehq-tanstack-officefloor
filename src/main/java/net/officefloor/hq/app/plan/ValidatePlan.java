package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.ClaimValues;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the amount is one the claim form takes for a claim that corrects nothing (a number 0.00 or more
 *  with up to two decimals, see ClaimValues); the start and end are yyyy-MM-dd, the end not before the start; every N
 *  days is a whole number, at least 1. */
public class ValidatePlan {
    public void validate(@RequestBody PlanRequest request, Out<PlanValues> values) throws InvalidPlanException {
        if (request == null) {
            throw new InvalidPlanException("A plan is required");
        }
        ClaimValues claim;
        try {
            claim = ClaimValues.from(new ClaimRequest(request.amount(), null, "", null, request.by(), null, null));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        if (claim.amount().signum() < 0) {
            throw new InvalidPlanException("A plan's amount must be 0.00 or more");
        }
        LocalDate start = date(request.start(), "start");
        LocalDate end = date(request.end(), "end");
        if (end.isBefore(start)) {
            throw new InvalidPlanException("A plan's end must not be before its start");
        }
        String every = request.every() == null ? "" : request.every().trim();
        if (!every.matches("\\d{1,6}") || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("A plan's interval must be a whole number of days, at least 1");
        }
        values.set(new PlanValues(claim.amount(), start, Integer.parseInt(every), end, claim.by()));
    }

    private static LocalDate date(String typed, String what) throws InvalidPlanException {
        try {
            return LocalDate.parse(typed == null ? "" : typed.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("A plan's " + what + " must be yyyy-MM-dd");
        }
    }
}
