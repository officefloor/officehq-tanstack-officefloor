package net.officefloor.hq.app.claim;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaimRequest {

    /** An amount, negative for a correction, with at most two decimals. */
    private static final Pattern AMOUNT = Pattern.compile("-?(\\d+(\\.\\d{1,2})?|\\.\\d{1,2})");

    /** A claim number: sheet-wide (1, 2, ...) or within a period (P1-1, X-1). */
    private static final Pattern NUMBER = Pattern.compile("\\d{1,9}|.{1,110}-\\d{1,9}");

    public void validate(@RequestBody @Valid ClaimRequest request, Out<ClaimInput> input) throws InvalidClaimException {
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be a number with at most two decimals.");
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
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String corrects = request.corrects() == null ? "" : request.corrects().trim();
        String number = null;
        if (value.signum() < 0) {
            if (!NUMBER.matcher(corrects).matches()) {
                throw new InvalidClaimException("A negative amount is a correction: name the number of the claim it corrects.");
            }
            number = corrects;
        } else if (!corrects.isEmpty()) {
            throw new InvalidClaimException("Only a correction (a negative amount) corrects a claim.");
        }
        String ref = request.ref() == null ? "" : request.ref().strip();
        input.set(new ClaimInput(value, parsed, note, number, ref.isEmpty() ? null : ref));
    }
}
