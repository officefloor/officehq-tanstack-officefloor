package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rate form from the body; refuses a grade other than A, B or C and a percent that is missing, negative or has more than two decimals. */
public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    private static final Pattern PCT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<RateInput> body) throws InvalidRateException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        String pct = request == null || request.pct() == null ? "" : request.pct().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        if (!PCT.matcher(pct).matches()) {
            throw new InvalidRateException("The rate must be a percent, 0.00 or more, with up to two decimals.");
        }
        body.set(new RateInput(grade, new BigDecimal(pct).setScale(2)));
    }
}
