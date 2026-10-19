package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {

    /** 0 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body) throws InvalidClaimException {
        body.set(of(request));
    }

    /** The claim form's rules, shared with the quick-add bar. */
    static ValidClaim of(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().strip();
        String date = request == null || request.date() == null ? "" : request.date().strip();
        String note = request == null || request.note() == null ? "" : request.note();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more, with up to two decimals.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a valid date.");
            }
        }
        return new ValidClaim(new BigDecimal(amount).setScale(2), parsed, note);
    }
}
