package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;

/**
 * A validated claim request. {@code date} is null when left blank: the sheet's entry date while it is set, else its today.
 * {@code corrects} is the number of the claim a correction (a negative amount) corrects; null for
 * any other claim. {@code ref} is its external reference, trimmed; null when left blank.
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref) {
    private static final Pattern AMOUNT = Pattern.compile("-?\\d{1,15}(\\.\\d{1,2})?");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}|.{1,190}-\\d{1,9}");

    /** A claim request checked by the claim form's rules. */
    public static ClaimInput from(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) throw new InvalidClaimException("An amount is required");
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("An amount is a number with up to two decimals");
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("A date is yyyy-MM-dd");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 1000) throw new InvalidClaimException("A note is at most 1000 characters");
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String rawCorrects = request.corrects() == null ? "" : request.corrects().trim();
        String corrects = null;
        if (value.signum() < 0) {
            if (rawCorrects.isEmpty()) {
                throw new InvalidClaimException("A negative amount is a correction: name the claim it corrects");
            }
            if (!NUMBER.matcher(rawCorrects).matches()) {
                throw new InvalidClaimException("A correction names a claim by its number");
            }
            corrects = rawCorrects;
        } else if (!rawCorrects.isEmpty()) {
            throw new InvalidClaimException("Only a negative amount corrects a claim");
        }
        String ref = request.ref() == null ? "" : request.ref().trim();
        if (ref.length() > 200) throw new InvalidClaimException("A reference is at most 200 characters");
        return new ClaimInput(value, date, note, corrects, ref.isEmpty() ? null : ref);
    }
}
