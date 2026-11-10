package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {
    private static final Pattern AMOUNT = Pattern.compile("\\d{1,15}(\\.\\d{1,2})?");

    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> body)
            throws InvalidClaimException {
        String amount = request == null || request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) throw new InvalidClaimException("An amount is required");
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("An amount is a number of 0.00 or more, with up to two decimals");
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("A date is yyyy-MM-dd");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 1000) throw new InvalidClaimException("A note is at most 1000 characters");
        body.set(new ClaimInput(new BigDecimal(amount).setScale(2), date, note));
    }
}
