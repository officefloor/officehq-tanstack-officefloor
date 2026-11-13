package net.officefloor.hq.app.claim;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimRequest {

    /** 0.00 or more, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?|\\.\\d{1,2}");

    public void validate(@RequestBody @Valid ClaimRequest request, Out<ClaimInput> input) throws InvalidClaimException {
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be 0.00 or more, with at most two decimals.");
        }
        String date = request.date() == null ? "" : request.date().trim();
        LocalDate parsed = null;
        if (!date.isEmpty()) {
            try {
                parsed = LocalDate.parse(date);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be yyyy-mm-dd.");
            }
        }
        String note = request.note() == null ? "" : request.note().strip();
        input.set(new ClaimInput(new BigDecimal(amount).setScale(2), parsed, note));
    }
}
