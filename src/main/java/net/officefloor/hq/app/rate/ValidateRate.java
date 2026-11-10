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
        String rawFrom = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!rawFrom.isEmpty()) {
            try {
                from = LocalDate.parse(rawFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("A from-date is a date, yyyy-MM-dd");
            }
        }
        body.set(new RateInput(grade, new BigDecimal(pct).setScale(2), from));
    }
}
