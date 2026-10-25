package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A claim's fields: an amount (required; a number with up to two decimals, 0.00 or more, or below 0.00
 * for a correction), a date (blank for the sheet's today), a note (may be empty) and the number of the
 * claim a correction corrects (blank for none). Publishes them parsed, with the user making the change.
 */
public class ValidateClaim {

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claim,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        BigDecimal amount = ClaimAmount.signed(request == null ? null : request.amount());
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new BadRequestException("Enter a date as YYYY-MM-DD");
            }
        }
        if (note.length() > 1000) {
            throw new BadRequestException("A note is at most 1000 characters");
        }
        String correctsNumber = null;
        if (!corrects.isEmpty()) {
            if (!corrects.matches("\\d{1,9}|.{1,200}-\\d{1,9}")) {
                throw new BadRequestException("Name the corrected claim by its number");
            }
            correctsNumber = corrects.matches("\\d+") ? String.valueOf(Integer.parseInt(corrects)) : corrects;
        }
        claim.set(new ClaimInput(amount, parsed, note, correctsNumber));
        changedBy.set(by);
    }
}
