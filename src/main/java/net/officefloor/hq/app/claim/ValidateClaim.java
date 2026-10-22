package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: the amount is required, a
 *  number with up to two decimals, below 0.00 only for a correction, which names (by its number: a whole number, or <period name>-<n> / X-<n>) the claim it
 *  corrects, and only a correction names one; the date, when given, is a real yyyy-mm-dd date; a reference, when given, is
 *  at most 200 characters (whether it is already used on the sheet is checked once the sheet is read). */
public class ValidateClaim {
    public void validate(@RequestBody ClaimRequest request, Out<ClaimInput> body) throws InvalidClaimException {
        body.set(check(request));
    }

    /** The claim form's rules, shared by every way a claim is added (the paste box checks each line by them). */
    static ClaimInput check(ClaimRequest request) throws InvalidClaimException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidClaimException("Sign in before changing claims.");
        }
        String amount = request.amount() == null ? "" : request.amount().trim();
        if (amount.isEmpty()) {
            throw new InvalidClaimException("An amount is required.");
        }
        if (!amount.matches("-?\\d{1,12}(\\.\\d{1,2})?")) {
            throw new InvalidClaimException("The amount must be a number with up to two decimals.");
        }
        BigDecimal value = new BigDecimal(amount).setScale(2);
        String rawCorrects = request.corrects() == null ? "" : request.corrects().trim();
        String corrects = null;
        if (!rawCorrects.isEmpty()) {
            if (rawCorrects.matches("\\d{1,9}")) {
                corrects = String.valueOf(Integer.parseInt(rawCorrects));
            } else if (rawCorrects.matches(".{1,200}-\\d{1,9}")) {
                corrects = rawCorrects;
            } else {
                throw new InvalidClaimException("The claim corrected must be named by its number.");
            }
        }
        if (value.signum() < 0 && corrects == null) {
            throw new InvalidClaimException(
                    "A negative amount is a correction: name the number of the SETTLED claim it corrects.");
        }
        if (value.signum() >= 0 && corrects != null) {
            throw new InvalidClaimException("Only a correction (a negative amount) names a claim it corrects.");
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            if (!rawDate.matches("\\d{4}-\\d{2}-\\d{2}")) {
                throw new InvalidClaimException("The date must be a date (yyyy-mm-dd).");
            }
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date must be a real date (yyyy-mm-dd).");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        if (note.length() > 1000) {
            throw new InvalidClaimException("A note is at most 1000 characters.");
        }
        String ref = request.ref() == null ? "" : request.ref().trim();
        if (ref.length() > 200) {
            throw new InvalidClaimException("A reference is at most 200 characters.");
        }
        return new ClaimInput(request.user().trim(), value, date, note, corrects, ref.isEmpty() ? null : ref);
    }
}
