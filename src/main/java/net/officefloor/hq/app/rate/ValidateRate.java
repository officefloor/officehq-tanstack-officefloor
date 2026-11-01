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
    private static final Pattern PCT = Pattern.compile("\\d{1,7}(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> body) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        String from = request == null || request.from() == null ? "" : request.from().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more with up to two decimals");
        }
        LocalDate on;
        try {
            on = from.isEmpty() ? null : LocalDate.parse(from);
        } catch (DateTimeParseException e) {
            throw new InvalidRateException("Not a valid from-date: " + from);
        }
        body.set(new RateInput(grade, new BigDecimal(pct), on));
    }
}
