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

    /** 0 or more, with up to two decimals. */
    private static final Pattern PCT = Pattern.compile("(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");

    public void validate(@RequestBody RateRequest request, Out<ValidRate> body)
            throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("A percent of 0.00 or more with up to two decimals is required");
        }
        LocalDate from = date(request.from());
        LocalDate to = date(request.to());
        if (from != null && to != null && to.isBefore(from)) {
            throw new InvalidRateException("The to-date is before the from-date");
        }
        body.set(new ValidRate(grade, new BigDecimal(pct).setScale(2), from, to));
    }

    /** A blank date is null. */
    private static LocalDate date(String given) throws InvalidRateException {
        String text = given == null ? "" : given.trim();
        if (text.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(text);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a date: " + text);
        }
    }
}
