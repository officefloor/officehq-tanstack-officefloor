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
 * start; a blank start is the sheet's today, resolved when it is saved) and either every N days, a
 * whole number at least 1, or monthly on a day of the month, 1 to 31.
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
        String rawDay = request.day() == null ? "" : request.day().trim();
        Integer every = null;
        Integer day = null;
        if (!rawDay.isEmpty()) {
            if (!rawEvery.isEmpty()) throw new InvalidPlanException("A plan runs every N days or on a day of the month, not both");
            if (!rawDay.matches("\\d{1,2}") || Integer.parseInt(rawDay) < 1 || Integer.parseInt(rawDay) > 31) {
                throw new InvalidPlanException("The day of the month is a whole number from 1 to 31");
            }
            day = Integer.parseInt(rawDay);
        } else {
            if (!rawEvery.matches("\\d{1,6}") || Integer.parseInt(rawEvery) < 1) {
                throw new InvalidPlanException("Every is a whole number of days, at least 1");
            }
            every = Integer.parseInt(rawEvery);
        }
        if (start != null && end.isBefore(start)) throw new InvalidPlanException("A plan's end is not before its start");
        String sheet = request.sheet() == null || request.sheet().isBlank() ? null : request.sheet().trim();
        body.set(new PlanInput(claim.amount(), start, every, day, end, sheet));
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
