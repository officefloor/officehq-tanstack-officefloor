package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** A split needs an amount: a number with up to two decimals. */
public class ValidateSplit {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody SplitRequest request, Out<SplitAmount> body) throws InvalidClaimException {
        String raw = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(raw).matches()) {
            throw new InvalidClaimException("A split needs an amount with up to two decimals");
        }
        body.set(new SplitAmount(new BigDecimal(raw).setScale(2)));
    }
}
