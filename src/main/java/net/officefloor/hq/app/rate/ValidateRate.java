package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {

    private static final Set<String> GRADES = Set.of("A", "B", "C");

    /** A percent: 0 or more, at most four whole digits and two decimals. */
    private static final Pattern PERCENT = Pattern.compile("\\d{1,4}(\\.\\d{1,2})?");

    public void validate(@RequestBody RateRequest request, Out<ValidRate> body) throws InvalidRateException {
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidRateException("Grade must be A, B or C");
        }
        String raw = request.rate() == null ? "" : request.rate().trim();
        if (!PERCENT.matcher(raw).matches()) {
            throw new InvalidRateException("The rate must be a percent of 0.00 or more with at most two decimals");
        }
        body.set(new ValidRate(grade, new BigDecimal(raw).setScale(2), request.user()));
    }
}
