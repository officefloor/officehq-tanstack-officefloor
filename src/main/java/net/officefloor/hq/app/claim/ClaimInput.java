package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A validated claim body: a blank date already defaulted to the sheet's entry date (none set: its today); a negative amount (a correction) with
 * the number the claim it corrects shows (on the sheet, or within its period), which is null for any other claim; and its
 * external reference, trimmed (null when blank).
 */
public record ClaimInput(BigDecimal amount, LocalDate date, String note, String corrects, String ref) {

    public ClaimInput(BigDecimal amount, LocalDate date, String note) {
        this(amount, date, note, null, null);
    }

    /** The audit fields of the claim as added: {@link #auditFields} then ref=<reference> when it has one. */
    public String addedAuditFields(String person) {
        return auditFields(person) + (ref == null ? "" : " ref=" + ref);
    }

    /** Refuses a reference another claim on the sheet (any but the one excluded, null for none) already has, ignoring case. */
    public void checkRef(String sheet, Long excluded, Claims claims) throws InvalidClaimException {
        if (ref != null && claims.refUsed(sheet, ref, excluded)) {
            throw new InvalidClaimException("The reference " + ref + " is already used on the sheet");
        }
    }

    /** The audit fields of this claim for the named person: amount as the screen shows it, ISO date. */
    public String auditFields(String person) {
        return "person=" + person + " amount=" + amount.setScale(2, RoundingMode.UNNECESSARY).toPlainString()
                + " date=" + date;
    }

    /** Refuses a correction unless the claim it names is a SETTLED claim of the same person. */
    public void checkCorrects(String sheet, long person, Claims claims) throws InvalidClaimException {
        if (corrects == null) {
            return;
        }
        Claim corrected = claims.findByNumber(sheet, corrects);
        if (corrected == null || corrected.person() != person || corrected.status() != ClaimStatus.SETTLED) {
            throw new InvalidClaimException("A correction must name a SETTLED claim of the same person");
        }
    }
}
