package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * A claim's fields: an amount (required; a number with up to two decimals, 0.00 or more), a date
 * (blank for the sheet's today) and a note (may be empty). Publishes them parsed, with the user making the change.
 */
public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("\\d{1,12}(\\.\\d{1,2})?");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claim,
            Out<ChangedBy> changedBy) throws BadRequestException {
        ChangedBy by = ChangedBy.of(request == null ? null : request.by());
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (amount.isEmpty()) {
            throw new BadRequestException("Enter an amount");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new BadRequestException("An amount is a number, 0.00 or more, with up to two decimals");
        }
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
        claim.set(new ClaimInput(new BigDecimal(amount).setScale(2), parsed, note));
        changedBy.set(by);
    }
}
