package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the plan form from the body, trimmed: an amount of 0.00 or more with up to two decimals, a start and an end
 *  date, the end not before the start, and every N days, a whole number of at least 1. */
public class ValidatePlan {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final Pattern EVERY = Pattern.compile("\\d{1,6}");

    public void validate(@RequestBody(required = false) PlanRequest request, Out<PlanInput> body)
            throws InvalidPlanException {
        String amount = trim(request == null ? null : request.amount());
        String every = trim(request == null ? null : request.every());
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidPlanException("The amount must be 0.00 or more with up to two decimals.");
        }
        if (!EVERY.matcher(every).matches() || Integer.parseInt(every) < 1) {
            throw new InvalidPlanException("Every must be a whole number of days, at least 1.");
        }
        LocalDate start = date(request == null ? null : request.start(), "start");
        LocalDate end = date(request == null ? null : request.end(), "end");
        if (end.isBefore(start)) {
            throw new InvalidPlanException("The end must not be before the start.");
        }
        body.set(new PlanInput(new BigDecimal(amount).setScale(2), start, Integer.parseInt(every), end));
    }

    private static String trim(String field) {
        return field == null ? "" : field.trim();
    }

    private static LocalDate date(String field, String what) throws InvalidPlanException {
        try {
            return LocalDate.parse(trim(field));
        } catch (DateTimeParseException e) {
            throw new InvalidPlanException("The " + what + " must be a date.");
        }
    }
}
