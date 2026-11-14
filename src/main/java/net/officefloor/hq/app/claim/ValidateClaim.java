package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    /** 0 or more, with up to two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("(\\d+(\\.\\d{0,2})?|\\.\\d{1,2})");

    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body)
            throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        String date = request == null || request.date() == null ? "" : request.date().trim();
        String note = request == null || request.note() == null ? "" : request.note().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("An amount of 0.00 or more with up to two decimals is required");
        }
        LocalDate day = null;
        if (!date.isEmpty()) {
            try {
                day = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date: " + date);
            }
        }
        body.set(new ValidClaim(new BigDecimal(amount).setScale(2), day, note));
    }
}
