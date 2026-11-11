package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.hq.app.claim.ClaimRequest;
import net.officefloor.hq.app.claim.InvalidClaimException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A plan needs an amount the claim form would take, a start and an end date (the end not before the
 * start; a blank start is the sheet's today, resolved when it is saved) and every N days, a whole
 * number at least 1.
 */
public class ValidatePlan {
    public void validate(@RequestBody PlanRequest request, Out<PlanInput> body) throws InvalidPlanException {
        if (request == null) throw new InvalidPlanException("A plan needs an amount, a start, every N days and an end");
        ClaimInput claim;
        try {
            claim = ClaimInput.from(new ClaimRequest(request.amount(), "", "", "", "", ""));
        } catch (InvalidClaimException e) {
            throw new InvalidPlanException(e.getMessage());
        }
        LocalDate start = date(request.start(), "A start date");
        LocalDate end = date(request.end(), "An end date");
        if (end == null) throw new InvalidPlanException("An end date is required");
        String rawEvery = request.every() == null ? "" : request.every().trim();
        if (!rawEvery.matches("\\d{1,6}") || Integer.parseInt(rawEvery) < 1) {
            throw new InvalidPlanException("Every is a whole number of days, at least 1");
        }
        if (start != null && end.isBefore(start)) throw new InvalidPlanException("A plan's end is not before its start");
        body.set(new PlanInput(claim.amount(), start, Integer.parseInt(rawEvery), end));
    }

    private static LocalDate date(String raw, String what) throws InvalidPlanException {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) return null;
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException(what + " is yyyy-MM-dd");
        }
    }
}
