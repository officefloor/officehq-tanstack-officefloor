package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern PCT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> body)
            throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) throw new InvalidRateException("A grade is one of A, B or C");
        String pct = request.pct() == null ? "" : request.pct().trim();
        if (pct.isEmpty()) throw new InvalidRateException("A rate is required");
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("A rate is a percent of 0.00 or more, with up to two decimals");
        }
        LocalDate from = date(request.from(), "A from-date is a date, yyyy-MM-dd");
        LocalDate to = date(request.to(), "A to-date is a date, yyyy-MM-dd");
        if (to != null && from != null && to.isBefore(from)) {
            throw new InvalidRateException("A to-date is on or after the from-date");
        }
        body.set(new RateInput(grade, new BigDecimal(pct).setScale(2), from, to));
    }

    /** A date (yyyy-MM-dd), or null when blank. */
    private static LocalDate date(String raw, String message) throws InvalidRateException {
        String trimmed = raw == null ? "" : raw.trim();
        if (trimmed.isEmpty()) return null;
        try {
            return LocalDate.parse(trimmed);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException(message);
        }
    }
}
