package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

/**
 * A claim a person has on a sheet. {@code id} is null until the claim is first saved; {@code number}
 * is null unless the claim has been opened (a draft has none; a voided claim keeps it). {@code due} is the
 * due fixed when the claim was settled, or null while it is live (not settled, or settled with no rate).
 * {@code openedBy} is the user who opened the claim, or null while it is a draft. {@code stated} is whether
 * its person's last statement bound it to a due, {@code statedDue} (null: n/a) — its due as shown then, late
 * charge and floor included; it then shows and totals that due, not its live one. {@code number} is sheet-wide (1, 2, ...) or, under period numbering,
 * within its period (P1-1, X-1). {@code corrects} is, for a correction, the number of the claim it corrects (null:
 * not a correction). {@code dueBy} is fixed when the claim is created: the terms-th business day after its date (skipping Saturdays,
 * Sundays and the sheet's holidays) for its person's own terms, else those of their grade, else the sheet's, each in
 * force on that date; it does not change when the terms or its date change later. A PENDING claim is bound like any other claim that counts: while a statement binds it, it shows and
 * totals the bound figure. {@code deferredUntil} is the date an OPEN claim is
 * deferred until (null: never deferred): while the sheet's today is before it the claim is {@link #deferred deferred}.
 * {@code proofs} is how many proofs the claim has been given. {@code splitFrom} is the number of the claim it was split
 * off (null: not split off another claim). {@code added} is when the claim was added to the sheet, on the sequence rates
 * are saved on (see {@link net.officefloor.hq.app.rate.Rate#appliesTo}): a forward-only rate applies only to claims added
 * after it was saved; {@link #NEWEST} for a claim not yet stored. {@code waived} is whether a steward waived its late
 * charge: a waived claim bears no late charge, whatever its overdue state. {@code held} is whether the claim is held:
 * a held OPEN or PENDING claim leaves the live totals as a hold on its person would (unless a statement binds it).
 */
public record Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
        String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs, String splitFrom, long added,
        boolean waived, boolean held) {

    /** The {@code added} of a claim not yet stored: added after every rate. */
    public static final long NEWEST = Long.MAX_VALUE;

    /** A claim that is not held. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs, String splitFrom, long added,
            boolean waived) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, deferredUntil, proofs, splitFrom, added, waived, false);
    }

    /** A claim whose late charge is not waived. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs, String splitFrom, long added) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, deferredUntil, proofs, splitFrom, added, false);
    }

    /** A claim not yet stored. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs, String splitFrom) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, deferredUntil, proofs, splitFrom, NEWEST);
    }

    /** A claim not split off another claim. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, deferredUntil, proofs, null);
    }

    /** A claim with no proofs. */
    public Claim(Long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
            ClaimStatus status, String number, BigDecimal due, String openedBy, boolean stated, BigDecimal statedDue,
            String corrects, LocalDate dueBy, LocalDate deferredUntil) {
        this(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue, corrects,
                dueBy, deferredUntil, 0);
    }

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
                statedDue, corrects, dueBy, deferredUntil, proofs, splitFrom, added, waived, held);
    }

    /** The same claim as an open claim bound by no statement: live, with no fixed due. */
    public Claim asOpenUnbound() {
        return new Claim(id, sheetId, personId, amount, date, note, ClaimStatus.OPEN, number, null, openedBy, false,
                null, corrects, dueBy, deferredUntil, proofs, splitFrom, added, waived, held);
    }

    /**
     * The claim keeping the rest of its amount once the split amount goes to a new claim: it loses any figure a
     * statement bound it to.
     */
    public Claim splitKeeping(BigDecimal rest) {
        return new Claim(id, sheetId, personId, rest, date, note, status, number, due, openedBy, false, null,
                corrects, dueBy, deferredUntil, proofs, splitFrom, added, waived, held);
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
     * Whether the claim is deferred on the given today: OPEN (or SETTLED while a statement bound it), deferred until a
     * date after that day, and bound by no statement. A deferred claim counts in no total, is not overdue, cannot be
     * settled and is left unbound by a statement; a claim deferred while bound stays in the totals and can be settled
     * until the binding ends (the statement is cancelled, or the next one leaves it unbound).
     */
    public boolean deferred(LocalDate today) {
        return (status == ClaimStatus.OPEN || status == ClaimStatus.SETTLED) && !bound() && deferredUntil != null
                && today != null && today.isBefore(deferredUntil);
    }

    /** The same claim deferred until the date. */
    public Claim deferTo(LocalDate until) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                corrects, dueBy, until, proofs, splitFrom, added, waived, held);
    }

    /** The same claim on another date with the given number, keeping its due-by and any statement figure. */
    public Claim redateTo(LocalDate nextDate, String nextNumber) {
        return new Claim(id, sheetId, personId, amount, nextDate, note, status, nextNumber, due, openedBy, stated,
                statedDue, corrects, dueBy, deferredUntil, proofs, splitFrom, added, waived, held);
    }

    /** The same claim with one more proof. */
    public Claim withProof() {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, due, openedBy, stated, statedDue,
                corrects, dueBy, deferredUntil, proofs + 1, splitFrom, added, waived, held);
    }

    /** Whether a proof can be added to the claim: it is neither SETTLED, VOID nor DELETED. */
    public boolean takesProof() {
        return status != ClaimStatus.SETTLED && status != ClaimStatus.VOID && status != ClaimStatus.DELETED;
    }

    /** Whether the claim is overdue on the given today: OPEN (or PENDING), not deferred, with its due-by before that day. */
    public boolean overdue(LocalDate today) {
        return overdue(today, 0);
    }

    /** Whether the claim is overdue on the given today with the grace days: its due-by plus them before that day. */
    public boolean overdue(LocalDate today, int grace) {
        return status.open() && !deferred(today) && dueBy != null && today != null
                && dueBy.plusDays(grace).isBefore(today);
    }

    /** Whether a statement binds the claim now: it was stated (PENDING included). */
    public boolean bound() {
        return stated;
    }

    /** Whether the claim keeps a figure whatever the rates: fixed when settled, or bound by a statement. */
    public boolean keepsFigure() {
        return due != null || (bound() && statedDue != null);
    }

    /**
     * The claim's figure (null: n/a): the due fixed when it was settled, else the due its statement bound
     * it to, else its amount at the given live rate (null: none) — no floor. Its live figure
     * rounds by the sheet's policy date.
     */
    public BigDecimal figure(BigDecimal liveRate, Rounding rounding) {
        if (due != null) {
            return due;
        }
        if (bound()) {
            return statedDue;
        }
        return due(amount, liveRate, rounding.modeFor(this));
    }

    /**
     * The claim's due (null: n/a): the due fixed when it was settled, else the due its statement bound it to, else
     * the greater of the floor in force on its date and its {@link #figure figure}, plus the late charge on that
     * floored figure while it is overdue. A settled claim's fixed due and a bound due already have the charge and floor in them; a
     * correction has no floor and no charge.
     */
    public BigDecimal due(BigDecimal liveRate, BigDecimal floor, LateCharge late, Rounding rounding) {
        BigDecimal figure = figure(liveRate, rounding);
        if (due != null || bound() || figure == null || correction()) {
            return figure;
        }
        BigDecimal floored = atLeast(figure, floor);
        return floored.add(late.on(this, floored, rounding.modeFor(this)));
    }

    /** An amount times a percent rate, divided by 100, rounded by the mode to two decimals; null with no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, RoundingMode mode) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, mode);
    }

    /** The value raised to the floor (null: no floor). */
    static BigDecimal atLeast(BigDecimal value, BigDecimal floor) {
        return floor == null || value.compareTo(floor) >= 0 ? value : floor.setScale(2);
    }

    /** The audit field for a claim's external reference, " ref=<reference>", or "" when it has none. */
    public static String auditRef(String ref) {
        return ref == null ? "" : " ref=" + ref;
    }

    /**
     * Tags as typed (comma separated) in their stored form: each trimmed and lower-cased, blanks and repeats dropped,
     * sorted and comma separated; null when there are none.
     */
    public static String tags(String typed) {
        if (typed == null) {
            return null;
        }
        String tags = Arrays.stream(typed.split(",")).map((t) -> t.strip().toLowerCase(Locale.ROOT))
                .filter((t) -> !t.isEmpty()).distinct().sorted().collect(Collectors.joining(","));
        return tags.isEmpty() ? null : tags;
    }

    /** The audit field for a claim's tags, " tags=<a,b>", or "" when it has none. */
    public static String auditTags(String tags) {
        return tags == null ? "" : " tags=" + tags;
    }

    /** The audit field for the claim's number, " number=<n>", or "" when it has none. */
    public static String auditNumber(String number) {
        return number == null ? "" : " number=" + number;
    }
}
