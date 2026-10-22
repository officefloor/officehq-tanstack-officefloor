package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written, by the rate form's rules (see RateRules). */
public class ValidateRate {
    public void validate(@PathVariable("grade") String grade, @RequestBody RateRequest request,
            Out<RateInput> body) throws InvalidRateException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidRateException("Sign in before changing rates.");
        }
        body.set(RateRules.check(request.user().trim(), grade, request));
    }
}
