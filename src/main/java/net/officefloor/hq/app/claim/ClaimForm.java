package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/** The claim form's rules for a claim's own fields: its amount, date, note, the claim it corrects and its reference. */
public final class ClaimForm {
    /** An amount (negative for a correction), with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("-?(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");
    /** A sheet-wide number, compared as a whole number (07 is 7). */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}");

    private ClaimForm() {
    }

    /** The request checked by the claim form's rules. */
    public static ValidClaim check(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        String ref = request == null || request.ref() == null ? "" : request.ref().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("An amount with up to two decimals is required");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String corrected = null;
        if (value.signum() < 0) {
            if (corrects.isEmpty()) {
                throw new InvalidClaimException("A negative amount must name the claim it corrects");
            }
            corrected = NUMBER.matcher(corrects).matches() ? String.valueOf(Long.parseLong(corrects)) : corrects;
        }
        LocalDate day = null;
        if (!date.isEmpty()) {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date: " + date);
            }
        }
        return new ValidClaim(value, day, note, corrected, ref.isEmpty() ? null : ref);
    }
}
