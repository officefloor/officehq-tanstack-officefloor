package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: a split needs the amount it splits off, a number with up to two decimals. */
public class ValidateSplit {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody SplitRequest request, Out<ClaimSplit> split) throws InvalidClaimStatusException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimStatusException("A split needs the amount it splits off, with up to two decimals");
        }
        split.set(new ClaimSplit(new BigDecimal(amount).setScale(2), request.by() == null ? "" : request.by().trim()));
    }
}
