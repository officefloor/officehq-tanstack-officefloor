package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rate from the body, refusing a grade other than A, B or C a percent that is not 0.00 or more with up to two decimals,
 *  or a from-date that is not a date. */
public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern RATE = Pattern.compile("\\d{1,5}(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> rateOut) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String rate = request == null || request.rate() == null ? "" : request.rate().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!RATE.matcher(rate).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more, with up to two decimals.");
        }
        String from = request.from() == null ? "" : request.from().trim();
        LocalDate fromDate = null;
        if (!from.isEmpty()) {
            try {
                fromDate = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidRateException("The from-date must be a date (yyyy-mm-dd).");
            }
        }
        rateOut.set(new RateInput(grade, new BigDecimal(rate).setScale(2), fromDate));
    }
}
