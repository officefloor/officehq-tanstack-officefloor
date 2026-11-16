package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** A claim's values once validated. {@code date} is null when none was given (the sheet's today applies). {@code by} is who gives them. */
public record ClaimValues(BigDecimal amount, LocalDate date, String note, String by) {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    /** The request's values by the claim form's rules (see ValidateClaim). */
    public static ClaimValues from(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number 0.00 or more with up to two decimals");
        }
        String date = request.date() == null ? "" : request.date().trim();
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be yyyy-MM-dd");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        return new ClaimValues(new BigDecimal(amount).setScale(2), parsed, note,
                request.by() == null ? "" : request.by().trim());
    }
}
