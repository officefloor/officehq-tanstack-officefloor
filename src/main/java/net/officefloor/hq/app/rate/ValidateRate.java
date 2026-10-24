package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the rate form from the body; refuses a grade other than A, B or C and a percent that is missing, negative or has more than two decimals,
 * a from-date or to-date that is not a calendar date, and a to-date before the from-date. A blank from-date means from the beginning of time. */
public class ValidateRate {

    public void validate(@RequestBody RateRequest request, Out<RateInput> body) throws InvalidRateException {
        body.set(RateRules.check(request));
    }
}
