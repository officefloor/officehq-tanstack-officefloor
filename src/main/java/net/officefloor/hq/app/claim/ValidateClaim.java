package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    /** A number with up to two decimals; negative for a correction. */
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    /** A claim number: the sheet-wide 1, 2, ... or <period>-<n> / X-<n>. */
    private static final Pattern NUMBER = Pattern.compile("\\S{1,255}");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ClaimRequest request,
            SheetStore sheets, Out<ClaimInput> body) throws InvalidSheetException {
        body.set(check(sheetId, request, sheets));
    }

    /** The claim form's rules on a typed claim, before anything is stored: the claim as it would be stored. */
    static ClaimInput check(String sheetId, ClaimRequest request, SheetStore sheets) throws InvalidSheetException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number with up to two decimals");
        }
        String dateText = request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = dateText.isEmpty() ? sheets.today(sheetId.trim()) : LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + dateText);
        }
        String note = request.note() == null ? "" : request.note().trim();
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String correctsText = request.corrects() == null ? "" : request.corrects().trim();
        String corrects = null;
        if (value.signum() < 0) {
            if (!NUMBER.matcher(correctsText).matches()) {
                throw new InvalidSheetException("A negative amount must name the number of the settled claim it corrects");
            }
            corrects = correctsText;
        } else if (!correctsText.isEmpty()) {
            throw new InvalidSheetException("Only a negative amount corrects a claim");
        }
        return new ClaimInput(value, date, note, request.user(), corrects);
    }
}
