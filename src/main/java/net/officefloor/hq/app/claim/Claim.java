package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A claim a person has on a sheet. {@code id} is null until the claim is first saved; {@code number}
 * is null unless the claim has been opened (a draft has none, and voiding clears it). {@code due} is the
 * due fixed when the claim was settled, or null while it is live (not settled, or settled with no rate).
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number, BigDecimal due) {

    /** The same claim with another status, number and fixed due. */
    public Claim withStatus(ClaimStatus next, Integer nextNumber, BigDecimal nextDue) {
        return new Claim(id, sheetId, personId, amount, date, note, next, nextNumber, nextDue);
    }

    /** An amount times a percent rate, divided by 100, rounded half to even to two decimals; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** The audit field for the claim's number, " number=<n>", or "" when it has none. */
    public static String auditNumber(Integer number) {
        return number == null ? "" : " number=" + number;
    }
}
