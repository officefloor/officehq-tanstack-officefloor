package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    public void validate(@RequestBody RateRequest request, Out<ValidRate> body)
            throws InvalidRateException {
        body.set(RateForm.check(request));
    }
}
