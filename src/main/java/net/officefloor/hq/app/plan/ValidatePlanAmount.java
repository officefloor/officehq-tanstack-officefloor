package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the plan's new amount from the body, trimmed: 0.00 or more with up to two decimals. */
public class ValidatePlanAmount {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody(required = false) PlanAmountRequest request, Out<PlanAmountInput> body)
            throws InvalidPlanException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidPlanException("The amount must be 0.00 or more with up to two decimals.");
        }
        body.set(new PlanAmountInput(new BigDecimal(amount).setScale(2)));
    }
}
