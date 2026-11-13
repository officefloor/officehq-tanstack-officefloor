package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

/**
 * A claim a person has on a sheet. {@code id} is null until the claim is first saved; {@code number}
 * is null unless the claim has been opened (a draft has none; a voided claim keeps it). {@code due} is the
 * due fixed when the claim was settled, or null while it is live (not settled, or settled with no rate).
 * {@code openedBy} is the user who opened the claim, or null while it is a draft. {@code stated} is whether
 * its person's last statement bound it to a figure, {@code statedDue} (null: n/a) — it then shows and totals
 * that figure, not its live one.
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue) {

    /** The same claim with another status, number, fixed due and opener. */
    public Claim withStatus(ClaimStatus next, Integer nextNumber, BigDecimal nextDue, String nextOpenedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, next, nextNumber, nextDue, nextOpenedBy, stated,
                statedDue);
    }

    /** Whether the claim counts toward the totals: only OPEN and SETTLED claims do. */
    public boolean counts() {
        return status == ClaimStatus.OPEN || status == ClaimStatus.SETTLED;
    }

    /** Whether the claim counts toward the live totals when its person is held or not: a held person's open claims do not. */
    public boolean counts(boolean held) {
        return status == ClaimStatus.SETTLED || (status == ClaimStatus.OPEN && !held);
    }

    /**
     * The claim's figure (null: n/a): the due fixed when it was settled, else the figure its statement bound
     * it to, else its amount at the given live rate (null: none), at least the sheet's floor.
     */
    public BigDecimal figure(BigDecimal liveRate, BigDecimal floor) {
        if (due != null) {
            return due;
        }
        return stated ? statedDue : due(amount, liveRate, floor);
    }

    /** An amount times a percent rate, divided by 100, rounded half to even to two decimals; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** A live due: {@link #due(BigDecimal, BigDecimal)}, raised to the floor; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, BigDecimal floor) {
        BigDecimal due = due(amount, rate);
        return due == null || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }

    /** The audit field for the claim's number, " number=<n>", or "" when it has none. */
    public static String auditNumber(Integer number) {
        return number == null ? "" : " number=" + number;
    }
}
