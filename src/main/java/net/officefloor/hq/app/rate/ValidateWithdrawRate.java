package net.officefloor.hq.app.rate;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateWithdrawRate {
    public void validate(@RequestBody WithdrawRateRequest request, Out<WithdrawnRate> body)
            throws InvalidRateException {
        body.set(WithdrawRateRequest.checked(request));
    }
}
