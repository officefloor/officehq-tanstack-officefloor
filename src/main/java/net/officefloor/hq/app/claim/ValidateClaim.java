package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the claim from the body, refusing an amount that is not 0.00 or more with up to two decimals, or a bad date. */
public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> claimOut) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more, with up to two decimals.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be a date (yyyy-mm-dd).");
            }
        }
        claimOut.set(new ClaimInput(new BigDecimal(amount).setScale(2), parsed, note));
    }
}
