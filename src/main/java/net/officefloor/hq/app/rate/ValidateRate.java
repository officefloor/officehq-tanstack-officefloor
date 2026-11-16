package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the grade is A, B or C; the rate a percent 0.00 or more with up to two decimals;
 *  the from-date blank (none) or a date yyyy-MM-dd. */
public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");
    private static final Pattern RATE = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?");

    public void validate(@PathVariable("grade") String grade, @RequestBody RateRequest request, Out<RateValues> values)
            throws InvalidRateException {
        String g = grade == null ? "" : grade.trim();
        if (!GRADES.contains(g)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        String rate = request == null || request.rate() == null ? "" : request.rate().trim();
        if (!RATE.matcher(rate).matches()) {
            throw new InvalidRateException("The rate must be a percent 0.00 or more with up to two decimals");
        }
        String fromText = request.from() == null ? "" : request.from().trim();
        LocalDate from = null;
        if (!fromText.isEmpty()) {
            try {
                from = LocalDate.parse(fromText);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date yyyy-MM-dd");
            }
        }
        values.set(new RateValues(g, new BigDecimal(rate).setScale(2), from, request.by() == null ? "" : request.by().trim()));
    }
}
