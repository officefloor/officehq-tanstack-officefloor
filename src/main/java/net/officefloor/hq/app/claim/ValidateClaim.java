package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: the amount is required, a
 *  number 0.00 or more with up to two decimals; the date, when given, is a real yyyy-mm-dd date. */
public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> body) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!amount.matches("\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("The amount must be a number, 0.00 or more, with up to two decimals.");
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            if (!rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new InvalidClaimException("The date must be a date (yyyy-mm-dd).");
            }
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be a real date (yyyy-mm-dd).");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 1000) {
            throw new InvalidClaimException("A note is at most 1000 characters.");
        }
        body.set(new ClaimInput(request.user().trim(), new BigDecimal(amount).setScale(2), date, note));
    }
}
