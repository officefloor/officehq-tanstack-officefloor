package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;

/** The rule for a claim's amount, shared by every form that enters one. */
public final class ClaimAmount {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    private ClaimAmount() {
    }

    /** A claim's amount: required; a number with up to two decimals, 0.00 or more. */
    public static BigDecimal of(String typed) throws BadRequestException {
        String amount = typed == null ? "" : typed.trim();
        if (amount.isEmpty()) {
            throw new BadRequestException("Enter an amount");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new BadRequestException("An amount is a number, 0.00 or more, with up to two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }
}
