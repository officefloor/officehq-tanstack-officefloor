package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.regex.Pattern;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateClaim {

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?");
    private static final Pattern SIGNED_AMOUNT = Pattern.compile("-?\\d+(\\.\\d{1,2})?");
    private static final Pattern NUMBER = Pattern.compile("\\d+|\\S+-\\d+");

    public void validate(@RequestBody ClaimRequest request, Out<ValidClaim> body) throws InvalidClaimException {
        body.set(check(request));
    }

    /** The claim form's rules on its body alone; the sheet's rules are checked when the claim is saved. */
    static ValidClaim check(ClaimRequest request) throws InvalidClaimException {
        BigDecimal amount = signedAmount(request.amount());
        String corrects = corrects(request.corrects());
        if (amount.signum() < 0 && corrects == null) {
            throw new InvalidClaimException("A negative amount is a correction: it must name the claim it corrects");
        }
        if (amount.signum() >= 0 && corrects != null) {
            throw new InvalidClaimException("Only a correction (a negative amount) can name a claim it corrects");
        }
        String rawDate = request.date() == null ? "" : request.date().trim();
        LocalDate date = null;
        if (!rawDate.isEmpty()) {
            try {
                date = LocalDate.parse(rawDate);
            } catch (DateTimeParseException e) {
                throw new InvalidClaimException("The date is not a date");
            }
        }
        String note = request.note() == null ? "" : request.note().trim();
        return new ValidClaim(amount, date, note, request.user(), corrects);
    }

    /** The claim form's amount: with at most two decimals; a negative one is a correction. */
    static BigDecimal signedAmount(String raw) throws InvalidClaimException {
        String amount = raw == null ? "" : raw.trim();
        if (!SIGNED_AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must have at most two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }

    /** The number of the claim a correction corrects; null when blank. */
    static String corrects(String raw) throws InvalidClaimException {
        String corrects = raw == null ? "" : raw.trim();
        if (corrects.isEmpty()) return null;
        if (!NUMBER.matcher(corrects).matches()) {
            throw new InvalidClaimException("The claim corrected must be named by its number");
        }
        return corrects;
    }

    /** Refuse a correction that does not name a SETTLED claim of the same person. */
    static void checkCorrects(ValidClaim request, String sheetId, long personId, Claims claims) throws InvalidClaimException {
        if (request.corrects() != null && !claims.isSettled(sheetId, personId, request.corrects())) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person");
        }
    }

    /** A claim's amount: 0.00 or more, with at most two decimals. */
    static BigDecimal amount(String raw) throws InvalidClaimException {
        String amount = raw == null ? "" : raw.trim();
        if (!AMOUNT.matcher(amount).matches()) {
            throw new InvalidClaimException("The amount must be 0.00 or more, with at most two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }
}
