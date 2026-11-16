package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the amount is required, a number with up to two decimals (negative only for a correction); the date is blank or yyyy-MM-dd. */
public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimValues> values) throws InvalidClaimException {
        values.set(ClaimValues.from(request));
    }
}
