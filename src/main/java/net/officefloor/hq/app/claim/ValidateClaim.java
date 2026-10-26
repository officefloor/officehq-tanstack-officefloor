package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");

    private static final DateTimeFormatter ISO = DateTimeFormatter.ofPattern("uuuu-MM-dd")
            .withResolverStyle(ResolverStyle.STRICT);

    // The amount is required; a blank date takes the sheet's today (filled in when stored); the note may be empty.
    public void validate(@RequestBody ClaimRequest body, Out<ClaimInput> input) throws InvalidClaimException {
        String amount = body == null || body.amount() == null ? "" : body.amount().trim();
        String date = body == null || body.date() == null ? "" : body.date().trim();
        String note = body == null || body.note() == null ? "" : body.note().trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number of 0.00 or more, with up to two decimals.");
        }
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date, ISO);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("Not a date (yyyy-mm-dd): " + date);
            }
        }
        input.set(new ClaimInput(new BigDecimal(amount).setScale(2), parsed, note));
    }
}
