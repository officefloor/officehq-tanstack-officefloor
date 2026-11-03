package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Pattern;
import net.officefloor.hq.app.BadRequestException;

/** A validated claim form: the amount (negative for a correction), the date ({@code null} when left blank:
 *  the sheet's today), the note, and the number of the claim a correction corrects ({@code null} when left blank), and its external reference (trimmed;
 *  {@code null} when left blank), and its tags (see {@link Claim#tags(String)}; {@code null} when none). */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref, String tags) {

    private static final Pattern SIGNED = Pattern.compile("-?(\\d+(\\.\\d{1,2})?|\\.\\d{1,2})");

    private static final Pattern AMOUNT = Pattern.compile("\\d+(\\.\\d{1,2})?|\\.\\d{1,2}");

    /** A typed amount (trimmed): required, a number with up to two decimals, 0.00 or more. */
    public static BigDecimal amount(String amount) throws BadRequestException {
        if (amount.isEmpty()) {
            throw new BadRequestException("amount is required");
        }
        if (!AMOUNT.matcher(amount).matches()) {
            throw new BadRequestException("amount must be a number of 0.00 or more with up to two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }

    /** A typed claim-form amount (trimmed): required, a number with up to two decimals; a negative one
     *  is a correction. */
    public static BigDecimal signedAmount(String amount) throws BadRequestException {
        if (amount.isEmpty()) {
            throw new BadRequestException("amount is required");
        }
        if (!SIGNED.matcher(amount).matches()) {
            throw new BadRequestException("amount must be a number with up to two decimals");
        }
        return new BigDecimal(amount).setScale(2);
    }

    /** Check the external reference against the sheet: one already used by another claim on it (other than
     *  {@code exceptId}, null: none), comparing without regard to case, is refused. */
    public void checkRef(ClaimRepository repository, String sheetId, Long exceptId) throws BadRequestException {
        if (ref != null && repository.refUsed(sheetId, ref, exceptId)) {
            throw new BadRequestException("reference " + ref + " is already used on the sheet");
        }
    }

    /** Whether the amount is a correction's: below zero. */
    public boolean correction() {
        return amount.signum() < 0;
    }

    /** Check the form against the person's claims: a negative amount must name, by number, a SETTLED
     *  claim of the same person; only a negative amount may name one. */
    public void checkCorrection(ClaimRepository repository, String sheetId, long personId) throws BadRequestException {
        if (!correction()) {
            if (corrects != null) {
                throw new BadRequestException("only a negative amount corrects a claim");
            }
            return;
        }
        if (corrects == null) {
            throw new BadRequestException("a negative amount is a correction: name the settled claim it corrects");
        }
        if (!repository.settledOf(sheetId, personId, corrects)) {
            throw new BadRequestException("claim " + corrects + " is not a settled claim of this person");
        }
    }
}
