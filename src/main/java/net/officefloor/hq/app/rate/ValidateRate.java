package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateRate {
    public void validate(@RequestBody RateRequest request, Out<RateInput> body)
            throws InvalidRateException {
        body.set(RateRequest.checked(request));
    }
}
