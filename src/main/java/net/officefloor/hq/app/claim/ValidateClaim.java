package net.officefloor.hq.app.claim;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the claim form: an amount is required (a number with up to two decimals; a negative one is a
 *  correction), a blank date takes the sheet's entry date (its today when none is set), the note may be empty, and corrects is blank or
 *  the number of the claim a correction corrects, and the external reference is trimmed (blank: none). */
public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claim) throws BadRequestException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        String number = corrects.isEmpty() ? null : corrects;
        String ref = request == null || request.ref() == null ? "" : request.ref().trim();
        LocalDate on = null;
        if (!date.isEmpty()) {
            try {
                on = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("date must be yyyy-mm-dd");
            }
        }
        claim.set(new ClaimInput(ClaimInput.signedAmount(amount), on, note, number, ref.isEmpty() ? null : ref));
    }
}
