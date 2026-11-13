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
 * that figure, not its live one. {@code corrects} is, for a correction, the number of the claim it corrects (null:
 * not a correction).
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, Integer number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
        Integer corrects) {

    /** The same claim with another status, number, fixed due and opener. */
    public Claim withStatus(ClaimStatus next, Integer nextNumber, BigDecimal nextDue, String nextOpenedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, next, nextNumber, nextDue, nextOpenedBy, stated,
                statedDue, corrects);
    }

    /** Whether the claim is a correction: a negative amount naming the settled claim it corrects. */
    public boolean correction() {
        return corrects != null;
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
     * it to, else its amount at the given live rate (null: none), at least the sheet's floor — a correction has
     * no floor. Its live figure rounds by the sheet's policy date.
     */
    public BigDecimal figure(BigDecimal liveRate, BigDecimal floor, Rounding rounding) {
        if (due != null) {
            return due;
        }
        if (stated) {
            return statedDue;
        }
        RoundingMode mode = rounding.modeFor(this);
        return correction() ? due(amount, liveRate, mode) : due(amount, liveRate, floor, mode);
    }

    /**
     * The claim's due (null: n/a): its {@link #figure figure}, plus the late charge on it while it is overdue. A
     * settled claim's fixed due already has the charge frozen in.
     */
    public BigDecimal due(BigDecimal liveRate, BigDecimal floor, LateCharge late, Rounding rounding) {
        BigDecimal figure = figure(liveRate, floor, rounding);
        return due != null || figure == null ? figure : figure.add(late.on(this, figure, rounding.modeFor(this)));
    }

    /** An amount times a percent rate, divided by 100, rounded by the mode to two decimals; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, RoundingMode mode) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, mode);
    }

    /** A live due: {@link #due(BigDecimal, BigDecimal, RoundingMode)}, raised to the floor; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, BigDecimal floor, RoundingMode mode) {
        BigDecimal due = due(amount, rate, mode);
        return due == null || due.compareTo(floor) >= 0 ? due : floor.setScale(2);
    }

    /** The audit field for the claim's number, " number=<n>", or "" when it has none. */
    public static String auditNumber(Integer number) {
        return number == null ? "" : " number=" + number;
    }
}
