package net.officefloor.hq.app.claim;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the claim form from the body; refuses an amount that is missing or has more than two decimals, a date
 * that is not a calendar date, and a corrects that is not a claim number. A negative amount (a correction) must
 * name the claim it corrects, and only a negative amount may. A blank date is left for the sheet's today.
 */
public class ValidateClaim {

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> body) throws InvalidClaimException {
        body.set(request == null ? ClaimRules.check(null, null, null, null)
                : ClaimRules.check(request.amount(), request.date(), request.note(), request.corrects()));
    }
}
