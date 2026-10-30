package net.officefloor.hq.app.claim;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A claim made by a person on a sheet. {@code amount} is 0.00 or more, in two decimals. {@code number}
 * is given when the claim is settled or approved, and kept by a void (null before; a claim numbered on opening before keeps its number). {@code settledDue} is the due fixed
 * when it was settled (null when not settled, or settled while no rate of its grade applied on its date).
 * {@code openedBy} is the user who opened it (null when never opened). {@code stated} is whether its person's
 * last statement bound it, at {@code statedDue} (null when it had no figure then). A claim with a negative amount is a
 * correction: {@code corrects} is the number of the SETTLED claim of the same person it corrects (null for any other claim).
 * {@code dueBy} is fixed when the claim is made, the terms-th business day after its date (see {@code Holidays#dueBy}), and kept when its terms or its date change later; a holiday added later on or before it works it out again from its date with the terms it was fixed with (see {@code Claims#refixDueBys}); one a steward set directly stays as set (see {@code Claims#setDueBy}).
 * {@code deferredUntil} is the date an OPEN claim was deferred to (null when never deferred).
 * {@code proofs} is how many proofs it has been given (0 for none), {@code proofsPhoto} how many of them are photos (the rest are receipts).
 * {@code splitFrom} is the number of the claim it was split off (null for any other claim).
 * {@code lateEntry} is whether it was added while the period its due-by falls in was soft-closed.
 * {@code remindedOn} is the last sheet's today a reminder was written for it (null when never reminded).
 * {@code ref} is its external reference, unique on the sheet without regard to case (null when it has none).
 * {@code tags} are its tags, trimmed, lower-cased, without repeats and sorted (empty for none).
 * {@code waived} is whether a steward waived its late charge: it bears none from then on.
 * {@code held} is whether it is held: while OPEN or PENDING and not bound by a statement it counts in no live due figure (it still counts in claimed).
 */
public record Claim(long id, String sheetId, long personId, BigDecimal amount, LocalDate date, String note,
        ClaimStatus status, String number, BigDecimal settledDue, String openedBy, boolean stated,
        BigDecimal statedDue, String corrects, LocalDate dueBy, LocalDate deferredUntil, int proofs, int proofsPhoto,
        String splitFrom, boolean lateEntry, LocalDate remindedOn, String ref, java.util.List<String> tags, boolean waived, boolean held) {

    public Claim with(ClaimInput input, LocalDate date) {
        return new Claim(id, sheetId, personId, input.amount(), date, input.note(), status, number, settledDue, openedBy, stated, statedDue, input.corrects(), dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, input.ref(), input.tags(), waived, held);
    }

    public Claim withStatus(ClaimStatus status, String number, BigDecimal settledDue, String openedBy) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim moved to another person: unbound from any statement, so live until their next one. */
    public Claim withPerson(long personId) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim re-dated to {@code date}, numbered {@code number}. */
    public Claim withDate(LocalDate date, String number) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim due by {@code dueBy}. */
    public Claim withDueBy(LocalDate dueBy) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim deferred to {@code until}. */
    public Claim withDeferredUntil(LocalDate until) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, until, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim with {@code amount} as its amount, unbound from any statement (live until its person's next one). */
    public Claim withAmountUnbound(BigDecimal amount) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, false, null, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim with {@code proofs} proofs, {@code proofsPhoto} of them photos. */
    public Claim withProofs(int proofs, int proofsPhoto) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim with its late charge waived. */
    public Claim withWaived() {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, true, held);
    }

    /** The claim held ({@code held} true) or released. */
    public Claim withHeld(boolean held) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, remindedOn, ref, tags, waived, held);
    }

    /** The claim reminded on {@code on}. */
    public Claim withRemindedOn(LocalDate on) {
        return new Claim(id, sheetId, personId, amount, date, note, status, number, settledDue, openedBy, stated, statedDue, corrects, dueBy, deferredUntil, proofs, proofsPhoto, splitFrom, lateEntry, on, ref, tags, waived, held);
    }

    /**
     * Whether the claim is deferred on {@code today}: an OPEN claim deferred to a date the sheet's today has not reached
     * counts in no total, is not overdue, cannot be settled and is left unbound by a statement. While a statement binds
     * it the deferral waits: it counts and can be settled, and the deferral takes effect once the binding ends (on a
     * claim settled meanwhile too).
     */
    public boolean deferredOn(LocalDate today) {
        return (status == ClaimStatus.OPEN || status == ClaimStatus.SETTLED) && !stated && deferredUntil != null
                && (today == null || today.isBefore(deferredUntil));
    }

    /** How many of its proofs are of the kind (receipt or photo). */
    public int proofsOf(String kind) {
        return "photo".equals(kind) ? proofsPhoto : "receipt".equals(kind) ? proofs - proofsPhoto : 0;
    }

    /** A correction: its due is its amount times the rate, with no floor and no late charge. */
    public boolean isCorrection() {
        return amount.signum() < 0;
    }
}
