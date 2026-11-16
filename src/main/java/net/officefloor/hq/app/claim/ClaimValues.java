package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Locale;
import java.util.TreeSet;
import java.util.regex.Pattern;

/** A claim's values once validated. {@code date} is null when none was given (the sheet's today applies). {@code corrects} is the number of
 *  the claim a correction corrects (null when none was given). {@code by} is who gives them. {@code ref} is its external reference,
 *  surrounding spaces removed (null when none, or a blank one, was given). {@code tags} are its tags as stored (see #tags; null when none). */
public record ClaimValues(BigDecimal amount, LocalDate date, String note, String corrects, String by, String ref, String tags) {
    private static final Pattern AMOUNT = Pattern.compile("-?\\d{1,15}(\\.\\d{1,2})?");

    /** The request's values by the claim form's rules (see ValidateClaim). */
    public static ClaimValues from(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals");
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
        String corrects = request.corrects() == null ? "" : request.corrects().trim();
        String ref = request.ref() == null ? "" : request.ref().trim();
        return new ClaimValues(new BigDecimal(amount).setScale(2), parsed, note, corrects.isEmpty() ? null : corrects,
                request.by() == null ? "" : request.by().trim(), ref.isEmpty() ? null : ref,
                tags(request.tags()));
    }

    /** Comma separated tags as stored: each trimmed and in lower case, blanks and repeats dropped, sorted, joined by
     *  commas; null when none is left. */
    public static String tags(String typed) throws InvalidClaimException {
        if (typed == null) {
            return null;
        }
        String joined = String.join(",", new TreeSet<>(Arrays.stream(typed.split(",")).map((t) -> t.trim().toLowerCase(Locale.ROOT))
                .filter((t) -> !t.isEmpty()).toList()));
        if (joined.length() > 2000) {
            throw new InvalidClaimException("The tags are too long");
        }
        return joined.isEmpty() ? null : joined;
    }

    /** The reference as compared: without regard to case. */
    public static String refKey(String ref) {
        return ref == null ? null : ref.toLowerCase(Locale.ROOT);
    }
}
