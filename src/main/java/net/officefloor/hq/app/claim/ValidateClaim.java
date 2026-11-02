package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the claim form: an amount is required (a number with up to two decimals, 0.00 or more), a
 *  blank date takes the sheet's today, the note may be empty. */
public class ValidateClaim {
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claim) throws BadRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (amount.isEmpty()) {
            throw new BadRequestException("amount is required");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new BadRequestException("amount must be a number of 0.00 or more with up to two decimals");
        }
        LocalDate on = null;
        if (!date.isEmpty()) {
            try {
                on = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("date must be yyyy-mm-dd");
            }
        }
        claim.set(new ClaimInput(new BigDecimal(amount).setScale(2), on, note));
    }
}
