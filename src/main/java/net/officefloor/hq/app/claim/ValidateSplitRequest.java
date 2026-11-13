package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the split's body once for later steps; refuses (400) an amount that is missing or not 0.00 or more with at most two decimals. */
public class ValidateSplitRequest {
    public void validate(@RequestBody SplitRequest request, Out<SplitAmount> amount) throws InvalidClaimException {
        String typed = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!typed.matches("^(\\d+(\\.\\d{1,2})?|\\.\\d{1,2})$")) {
            throw new InvalidClaimException("Type the amount to split off as 0.00.");
        }
        amount.set(new SplitAmount(new BigDecimal(typed).setScale(2)));
    }
}
