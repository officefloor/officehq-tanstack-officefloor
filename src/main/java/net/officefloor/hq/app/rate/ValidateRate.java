package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rate from the body, refusing a grade other than A, B or C or a percent that is not 0.00 or more with up to two decimals. */
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
        rateOut.set(new RateInput(grade, new BigDecimal(rate).setScale(2)));
    }
}
