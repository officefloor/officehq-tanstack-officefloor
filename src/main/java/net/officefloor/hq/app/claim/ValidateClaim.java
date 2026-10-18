package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the claim from the body, refusing an amount that is not a number with up to two decimals, or a bad date.
 *  A negative amount is a correction and must name, by number, the claim it corrects; any other claim names none. */
public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claimOut) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (!corrects.isEmpty() && !NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("The claim corrected must be given by its number.");
        }
        if (value.signum() < 0 && corrects.isEmpty()) {
            throw new InvalidClaimException("A negative amount is a correction: name the settled claim it corrects.");
        }
        if (value.signum() >= 0 && !corrects.isEmpty()) {
            throw new InvalidClaimException("Only a correction (a negative amount) names a claim it corrects.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be a date (yyyy-mm-dd).");
            }
        }
        claimOut.set(new ClaimInput(value, parsed, note,
                corrects.isEmpty() ? null : Integer.valueOf(corrects)));
    }
}
