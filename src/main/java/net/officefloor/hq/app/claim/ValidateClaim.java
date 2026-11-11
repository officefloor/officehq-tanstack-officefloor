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
    /** 0.00 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody ClaimRequest request,
            SheetStore sheets, Out<ClaimInput> body) throws InvalidSheetException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidSheetException("The amount must be a number of 0.00 or more with up to two decimals");
        }
        String dateText = request.date() == null ? "" : request.date().trim();
        LocalDate date;
        try {
            date = dateText.isEmpty() ? sheets.today(sheetId.trim()) : LocalDate.parse(dateText);
        } catch (DateTimeParseException e) {
            throw new InvalidSheetException("Not a date: " + dateText);
        }
        String note = request.note() == null ? "" : request.note().trim();
        body.set(new ClaimInput(new BigDecimal(amount).setScale(2), date, note, request.user()));
    }
}
