package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@PathVariable("sheet") String sheet, @RequestBody ClaimRequest request, SheetClock clock,
            Out<ClaimInput> body) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more with up to two decimals");
        }
        LocalDate on;
        try {
            on = date.isEmpty() ? clock.today(sheet) : LocalDate.parse(date);
        } catch (DateTimeParseException e) {
            throw new InvalidClaimException("Not a valid date: " + date);
        }
        body.set(new ClaimInput(new BigDecimal(amount), on, note));
    }
}
