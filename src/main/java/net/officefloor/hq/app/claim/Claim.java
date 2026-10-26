package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim made by a person on a sheet. {@code amount} is 0.00 or more, in two decimals. {@code number}
 * is given when the claim is opened (null for a draft or a voided claim). {@code settledDue} is the due fixed
 * when it was settled (null when not settled, or settled while no rate of its grade applied on its date).
 * {@code openedBy} is the user who opened it (null when never opened).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Long number, BigDecimal settledDue, String openedBy) {

    public Claim with(ClaimInput input, LocalDate date) {
        return new Claim(id, sheetId, personId, input.amount(), date, input.note(), status, number, settledDue, openedBy);
    }

    public Claim withStatus(ClaimStatus status, Long number, BigDecimal settledDue, String openedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy);
    }
}
