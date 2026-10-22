package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: the amount is a number, not below 0.00, with up to
 *  two decimals; the start and end are real yyyy-mm-dd dates, the end not before the start; every is a whole number
 *  of days, at least 1. */
public class ValidatePlan {
    public void validate(@RequestBody PlanRequest request, Out<PlanInput> body) throws InvalidPlanException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPlanException("Sign in before adding plans.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!amount.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidPlanException("The amount must be a number, not below 0.00, with up to two decimals.");
        }
        LocalDate start = date(request.start(), "start");
        String every = request.every() == null ? "" : request.every().trim();
        if (!every.matches("\\d{1,9}") || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("Every must be a whole number of days, at least 1.");
        }
        LocalDate end = date(request.end(), "end");
        if (end.isBefore(start)) {
            throw new InvalidPlanException("The end may not be before the start.");
        }
        body.set(new PlanInput(request.user().trim(), new BigDecimal(amount).setScale(2), start, Integer.parseInt(every),
                end));
    }

    private static LocalDate date(String typed, String what) throws InvalidPlanException {
        String raw = typed == null ? "" : typed.trim();
        if (!raw.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new InvalidPlanException("The " + what + " must be a date (yyyy-mm-dd).");
        }
        try {
            return LocalDate.parse(raw);
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("The " + what + " must be a real date (yyyy-mm-dd).");
        }
    }
}
