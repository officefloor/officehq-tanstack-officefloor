package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body)
            throws InvalidClaimException {
        body.set(ClaimForm.check(request));
    }
}
