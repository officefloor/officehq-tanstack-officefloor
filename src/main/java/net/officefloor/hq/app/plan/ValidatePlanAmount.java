package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, and an amount that is a number, not below
 *  0.00, with up to two decimals (as a plan is added with). */
public class ValidatePlanAmount {
    public void validate(@RequestBody PlanAmountRequest request, Out<PlanAmountInput> body) throws InvalidPlanException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPlanException("Sign in before changing plans.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (!amount.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidPlanException("The amount must be a number, not below 0.00, with up to two decimals.");
        }
        body.set(new PlanAmountInput(request.user().trim(), new BigDecimal(amount).setScale(2)));
    }
}
