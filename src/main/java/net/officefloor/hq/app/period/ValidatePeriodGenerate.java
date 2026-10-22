package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: from and to real yyyy-mm-dd dates, from on or
 *  before to, and no more than MAX_MONTHS months. */
public class ValidatePeriodGenerate {

    static final int MAX_MONTHS = 1200;

    public void validate(@RequestBody PeriodGenerateRequest request, Out<PeriodGenerateInput> body)
            throws InvalidPeriodException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPeriodException("Sign in before generating periods.");
        }
        LocalDate from = date(request.from(), "from");
        LocalDate to = date(request.to(), "to");
        if (to.isBefore(from)) {
            throw new InvalidPeriodException("The to-date must not be before the from-date.");
        }
        YearMonth first = YearMonth.from(from);
        YearMonth last = YearMonth.from(to);
        if (first.until(last, ChronoUnit.MONTHS) >= MAX_MONTHS) {
            throw new InvalidPeriodException("Generate at most " + MAX_MONTHS + " months at once.");
        }
        body.set(new PeriodGenerateInput(request.user().trim(), first, last));
    }

    private static LocalDate date(String text, String which) throws InvalidPeriodException {
        try {
            return LocalDate.parse(text == null ? "" : text.trim());
        } catch (DateTimeParseException e) {
            throw new InvalidPeriodException("The " + which + "-date must be a date, yyyy-mm-dd.");
        }
    }
}
