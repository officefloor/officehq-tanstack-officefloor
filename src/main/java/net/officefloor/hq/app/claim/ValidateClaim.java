package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {

    /** Any amount, with up to two decimals; a negative one is a correction. */
    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    /** A claim number: 1 or more. */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}");

    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body) throws InvalidClaimException {
        body.set(of(request));
    }

    /** The claim form's rules, shared with the quick-add bar. A correction (a negative amount) must name the
     *  number of the claim it corrects, and only a correction may; whether that claim is a SETTLED claim of
     *  the same person is checked once the person is known (see {@link Correction}). */
    static ValidClaim of(ClaimRequest request) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().strip();
        String date = request == null || request.date() == null ? "" : request.date().strip();
        String note = request == null || request.note() == null ? "" : request.note();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().strip();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a valid date.");
            }
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        Integer number = null;
        if (value.signum() < 0) {
            if (corrects.isEmpty()) {
                throw new InvalidClaimException("A negative amount is a correction: name the number of the settled claim it corrects.");
            }
            if (!NUMBER.matcher(corrects).matches() || Integer.parseInt(corrects) < 1) {
                throw new InvalidClaimException("The claim a correction corrects must be named by its number.");
            }
            number = Integer.valueOf(corrects);
        } else if (!corrects.isEmpty()) {
            throw new InvalidClaimException("Only a claim with a negative amount corrects another claim.");
        }
        return new ValidClaim(value, parsed, note, number);
    }
}
