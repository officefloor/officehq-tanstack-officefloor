package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;

/** A validated claim form: the amount, the date ({@code null} when left blank: the sheet's today) and the note. */
public record ClaimInput(BigDecimal amount, LocalDate date, String note) {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?|\\.\\d{1,2}");

    /** A typed amount (trimmed): required, a number with up to two decimals, 0.00 or more. */
    public static BigDecimal amount(String amount) throws BadRequestException {
        if (amount.isEmpty()) {
            throw new BadRequestException("amount is required");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new BadRequestException("amount must be a number of 0.00 or more with up to two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }
}
