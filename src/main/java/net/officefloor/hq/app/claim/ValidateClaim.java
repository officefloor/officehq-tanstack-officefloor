package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Reads the claim form from the body; refuses an amount that is missing or has more than two decimals, a date
 * that is not a calendar date, and a corrects that is not a claim number. A negative amount (a correction) must
 * name the claim it corrects, and only a negative amount may. A blank date is left for the sheet's today.
 */
public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");

    private static final Pattern NUMBER = Pattern.compile("(.+-)?\\d{1,9}");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> body) throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        if (!corrects.isEmpty() && !NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("Corrects must be a claim number.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        if (value.signum() < 0 && corrects.isEmpty()) {
            throw new InvalidClaimException("A negative amount is a correction: name the settled claim it corrects.");
        }
        if (value.signum() >= 0 && !corrects.isEmpty()) {
            throw new InvalidClaimException("Only a correction (a negative amount) corrects a claim.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date: " + date);
            }
        }
        body.set(new ClaimInput(value, parsed, note, corrects.isEmpty() ? null : corrects));
    }
}
