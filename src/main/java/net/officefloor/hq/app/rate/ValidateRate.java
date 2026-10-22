package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: the grade is one of A, B or C,
 *  the rate a percent 0.00 or more with up to two decimals. */
public class ValidateRate {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@PathVariable("grade") String grade, @RequestBody RateRequest request,
            Out<RateInput> body) throws InvalidRateException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidRateException("Sign in before changing rates.");
        }
        String g = grade == null ? "" : grade.trim();
        if (!GRADES.contains(g)) {
            throw new InvalidRateException("The grade must be A, B or C.");
        }
        String rate = request.rate() == null ? "" : request.rate().trim();
        if (!rate.matches("\\d{1,3}(\\.\\d{1,2})?")) {
            throw new InvalidRateException("The rate must be a percent, 0.00 to 999.99, with up to two decimals.");
        }
        body.set(new RateInput(request.user().trim(), g, new BigDecimal(rate).setScale(2)));
    }
}
