package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    /** An amount (negative for a correction), with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("-?(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");
    private static final Pattern NUMBER = Pattern.compile("\\d{1,18}");

    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body)
            throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        String corrects = request == null || request.corrects() == null ? "" : request.corrects().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("An amount with up to two decimals is required");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        Long corrected = null;
        if (value.signum() < 0) {
            if (!NUMBER.matcher(corrects).matches()) {
                throw new InvalidClaimException("A negative amount must name the claim it corrects");
            }
            corrected = Long.valueOf(corrects);
        }
        LocalDate day = null;
        if (!date.isEmpty()) {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date: " + date);
            }
        }
        body.set(new ValidClaim(value, day, note, corrected));
    }
}
