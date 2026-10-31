package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;

/** Body of a claim add or edit, as typed: the amount, the date (blank for the sheet's today),
 *  the note and who makes the change. */
public record ClaimRequest(String amount, String date, String note, String user) {
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    /** The request's values as the claim rules accept them, or refused. */
    public static ClaimValues checked(ClaimRequest request) throws InvalidSheetRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetRequestException("The amount must be 0.00 or more, with up to two decimals");
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
        return new ClaimValues(new BigDecimal(amount).setScale(2), date, note, request.user());
    }
}
