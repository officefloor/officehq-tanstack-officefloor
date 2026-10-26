package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim made by a person on a sheet. {@code amount} is 0.00 or more, in two decimals. {@code number}
 * is given when the claim is opened (null for a draft or a voided claim). {@code settledDue} is the due fixed
 * when it was settled (null when not settled, or settled while no rate of its grade applied on its date).
 * {@code openedBy} is the user who opened it (null when never opened). {@code stated} is whether its person's
 * last statement bound it, at {@code statedDue} (null when it had no figure then). A claim with a negative amount is a
 * correction: {@code corrects} is the number of the SETTLED claim of the same person it corrects (null for any other claim).
 * {@code dueBy} is fixed when the claim is made, and kept when its terms or its date change later.
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects, LocalDate dueBy) {

    public Claim with(ClaimInput input, LocalDate date) {
        return new Claim(id, sheetId, personId, input.amount(), date, input.note(), status, number, settledDue, openedBy, stated, statedDue, input.corrects(), dueBy);
    }

    public Claim withStatus(ClaimStatus status, String number, BigDecimal settledDue, String openedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy);
    }

    /** The claim moved to another person: unbound from any statement, so live until their next one. */
    public Claim withPerson(long personId) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null, corrects, dueBy);
    }

    /** A correction: its due is its amount times the rate, with no floor and no late charge. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
