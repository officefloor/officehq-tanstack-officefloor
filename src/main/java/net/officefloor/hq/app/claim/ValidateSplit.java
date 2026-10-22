package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user and an amount with up to two decimals,
 *  above 0.00. */
public class ValidateSplit {
    public void validate(@RequestBody SplitRequest request, Out<SplitInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!amount.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("The amount to split off must be a number with up to two decimals.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() <= 0) {
            throw new InvalidClaimException("The amount to split off must be more than 0.00.");
        }
        body.set(new SplitInput(request.user().trim(), value));
    }
}
