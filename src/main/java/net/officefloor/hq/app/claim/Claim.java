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
 * that figure, not its live one. {@code number} is sheet-wide (1, 2, ...) or, under period numbering,
 * within its period (P1-1, X-1). {@code corrects} is, for a correction, the number of the claim it corrects (null:
 * not a correction). {@code dueBy} is fixed when the claim is created: its date plus the terms of its person's
 * grade in force on that date; it does not change when the terms or its date change later. While PENDING the claim is not bound: it shows and totals its live figure, and is bound again
 * once rejected (back to OPEN) when the statement included it. {@code deferredUntil} is the date an OPEN claim is
 * deferred until (null: never deferred): while the sheet's today is before it the claim is {@link #deferred deferred}.
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
        String corrects, LocalDate dueBy, LocalDate deferredUntil) {

    /** A claim never deferred. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, null);
    }

    /** The same claim with another status, number, fixed due and opener. */
    public Claim withStatus(ClaimStatus next, String nextNumber, BigDecimal nextDue, String nextOpenedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, next, nextNumber, nextDue, nextOpenedBy, stated,
                statedDue, corrects, dueBy, deferredUntil);
    }

    /** The same claim as an open claim bound by no statement: live, with no fixed due. */
    public Claim asOpenUnbound() {
        return new Claim(id, sheetId, personId, amount, date, note, ClaimStatus.OPEN, number, null, openedBy, false,
                null, corrects, dueBy, deferredUntil);
    }

    /** Whether the claim is a correction: a negative amount naming the settled claim it corrects. */
    public boolean correction() {
        return corrects != null;
    }

    /** Whether the claim counts toward the totals: only OPEN, PENDING and SETTLED claims do. */
    public boolean counts() {
        return status.open() || status == ClaimStatus.SETTLED;
    }

    /** Whether the claim counts toward the live totals when a hold on its person covers it or not: a held open (or pending) claim does not. */
    public boolean counts(boolean held) {
        return status == ClaimStatus.SETTLED || (status.open() && !held);
    }

    /**
     * Whether the claim is deferred on the given today: OPEN, deferred until a date after that day. A deferred claim
     * counts in no total, is not overdue, cannot be settled and is left unbound by a statement.
     */
    public boolean deferred(LocalDate today) {
        return status == ClaimStatus.OPEN && deferredUntil != null && today != null && today.isBefore(deferredUntil);
    }

    /** The same claim deferred until the date. */
    public Claim deferTo(LocalDate until) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                corrects, dueBy, until);
    }

    /** Whether the claim is overdue on the given today: OPEN (or PENDING), not deferred, with its due-by before that day. */
    public boolean overdue(LocalDate today) {
        return status.open() && !deferred(today) && dueBy != null && today != null && dueBy.isBefore(today);
    }

    /** Whether a statement binds the claim now: it was stated and is not PENDING. */
    public boolean bound() {
        return stated && status != ClaimStatus.PENDING;
    }

    /** Whether the claim keeps a figure whatever the rates: fixed when settled, or bound by a statement. */
    public boolean keepsFigure() {
        return due != null || (bound() && statedDue != null);
    }

    /**
     * The claim's figure (null: n/a): the due fixed when it was settled, else the figure its statement bound
     * it to (not while PENDING), else its amount at the given live rate (null: none), at least the sheet's floor — a correction has
     * no floor. Its live figure rounds by the sheet's policy date.
     */
    public BigDecimal figure(BigDecimal liveRate, BigDecimal floor, Rounding rounding) {
        if (due != null) {
            return due;
        }
        if (bound()) {
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
    public static String auditNumber(String number) {
        return number == null ? "" : " number=" + number;
    }
}
