package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the split body once, refusing one that names no amount with up to two decimals. */
public class ValidateSplit {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody SplitRequest request, Out<BigDecimal> amount)
            throws InvalidClaimStatusChangeException {
        String typed = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(typed).matches()) {
            throw new InvalidClaimStatusChangeException("A split names an amount with up to two decimals");
        }
        amount.set(new BigDecimal(typed).setScale(2));
    }
}
