package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;

/** Body of a claim add or edit, as typed: the amount, the date (blank for the sheet's today),
 *  the note, the number of the claim it corrects (blank for none), who makes the change and the
 *  external reference (blank for none). */
public record ClaimRequest(String amount, String date, String note, String corrects, String user, String ref) {
    /** Up to two decimals; negative only for a correction. */
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    private static final Pattern NUMBER = Pattern.compile("\\d+|.+-\\d+");

    /** The request's values as the claim rules accept them, or refused. A negative amount is a
     *  correction and must name the number of the claim it corrects; any other claim corrects none. */
    public static ClaimValues checked(ClaimRequest request) throws InvalidSheetRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetRequestException("The amount must be a number with up to two decimals");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String rawCorrects = request.corrects() == null ? "" : request.corrects().trim();
        String corrects = null;
        if (value.signum() < 0) {
            if (!NUMBER.matcher(rawCorrects).matches()) {
                throw new InvalidSheetRequestException("A negative amount must name the number of the settled claim it corrects");
            }
            corrects = rawCorrects;
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidSheetRequestException("Not a date (yyyy-mm-dd): " + rawDate);
            }
        }
        String note = request.note() == null ? "" : request.note();
        String ref = request.ref() == null || request.ref().isBlank() ? null : request.ref().trim();
        return new ClaimValues(value, date, note, corrects, request.user(), ref);
    }
}
