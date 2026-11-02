package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the claim form: an amount is required (a number with up to two decimals, 0.00 or more), a
 *  blank date takes the sheet's today, the note may be empty. */
public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claim) throws BadRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        LocalDate on = null;
        if (!date.isEmpty()) {
            try {
                on = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("date must be yyyy-mm-dd");
            }
        }
        claim.set(new ClaimInput(ClaimInput.amount(amount), on, note));
    }
}
