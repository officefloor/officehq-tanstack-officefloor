package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.claim.ClaimInput;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the plan form: an amount (more than zero, up to two decimals), a start date, every N days (a whole number,
 *  at least 1) and an end date not before the start are all required. */
public class ValidatePlan {
    public void validate(@RequestBody PlanRequest request, Out<PlanInput> plan) throws BadRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String every = request == null || request.every() == null ? "" : request.every().trim();
        BigDecimal value = ClaimInput.signedAmount(amount);
        if (value.signum() <= 0) {
            throw new BadRequestException("amount must be more than 0");
        }
        LocalDate start = date(request == null ? null : request.start(), "start date");
        LocalDate end = date(request == null ? null : request.end(), "end date");
        if (!every.matches("[0-9]{1,6}") || Integer.parseInt(every) < 1) {
            throw new BadRequestException("every must be a whole number of days, at least 1");
        }
        if (end.isBefore(start)) {
            throw new BadRequestException("end date must not be before start date");
        }
        plan.set(new PlanInput(value, start, Integer.parseInt(every), end));
    }

    private static LocalDate date(String value, String what) throws BadRequestException {
        String v = value == null ? "" : value.trim();
        if (v.isEmpty()) {
            throw new BadRequestException(what + " is required");
        }
        try {
            return LocalDate.parse(v);
        } catch (DateTimeParseException e) {
            throw new BadRequestException(what + " must be yyyy-mm-dd");
        }
    }
}
